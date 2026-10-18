# 03. 프로토콜 — REST · STOMP

프론트(`web/`)와 서버(`server/`)의 계약이다. 한쪽만 바꾸면 어긋나므로 **항상 같은 커밋에서 양쪽을 고친다.**

## 공통

- 인증: `Authorization: Bearer <JWT>` 헤더. 쿠키를 쓰지 않는다(프론트가 다른 사이트라 CSRF 를 막으려면 헤더가 맞다)
- STOMP 는 핸드셰이크에 헤더를 붙일 수 없어 **CONNECT 프레임의 `Authorization` 헤더**로 토큰을 보낸다
- **요청 본문에 사용자 id 를 받지 않는다.** 요청자는 서버가 토큰에서 확정한다
- 실패 응답은 전부 한 모양: `{ status, code, message, traceId? }` (`ErrorResponse`). 화면은 `message` 를 그대로 보여 준다

## REST (`/api/v1`)

| 메서드 | 경로 | 권한 | 하는 일 | 응답 |
| --- | --- | --- | --- | --- |
| POST | `/auth/visitors` | 없음 (IP 당 분당 5건) | 비로그인 신원 발급 | `{ accessToken }` |
| POST | `/auth/logout` | | 서버는 할 일 없음(토큰은 클라이언트가 버림) | 204 |
| GET | `/users/me` | 로그인 | 내 정보 + 등수(회원만) | `MyProfileResponse` |
| GET | `/users/{userId}` | 로그인 | 상대 프로필(닉네임·이미지·MMR·티어·visitor 여부) | `UserProfileResponse` |
| GET | `/rankings?limit=` | **없음** | 회원 상위 N명 (기본·최대 100). 동점은 같은 등수 | `RankingResponse[]` |
| GET | `/rooms` | 로그인 | 로비 목록. **최근에 만든 방부터** | `RoomSummaryResponse[]` |
| GET | `/rooms/{roomId}` | 로그인 + **그 방 사람만** | 방 상세. 남이면 403 이 아니라 404 (방 번호가 순증이라 존재가 드러남) | `RoomDetailResponse` |
| POST | `/rooms` | 로그인 | 방 생성 `{ title }` (공백 불가, 30자 이하) | 201 `{ roomId, title }` |
| POST | `/rooms/{roomId}/players` | 로그인 | 입장 | 201 |
| DELETE | `/rooms/{roomId}/players/me` | 로그인 | 나가기 | 204 |

"로그인" 은 `ROLE_USER`(카카오 회원) 또는 `ROLE_VISITOR`. 둘 다 같은 기능을 쓰고 차이는 정산·랭킹뿐이다([04](04-identity-and-rating.md)).

그 밖의 경로

| 경로 | 용도 |
| --- | --- |
| `/oauth2/authorization/kakao` | 카카오 로그인 시작 (브라우저가 직접 이동) |
| `/api/oauth2/kakao` | 카카오 콜백 |
| `/ws` | STOMP over WebSocket |
| `/actuator/health/**`, `/actuator/prometheus` | 프로브·메트릭. 인그레스에 노출하지 않음 |

## STOMP

목적지 문자열은 서버 `messaging/StompDestinations`, 프론트 `web/src/lib/socket.ts` 두 곳에만 있다.

### 클라이언트 → 서버 (`/app/rooms/{roomId}/{행동}`)

roomId 는 항상 **경로**에서 받는다. 페이로드에 식별자를 넣으면 조작 지점이 된다.

| 행동 | 본문 | 누가 | 결과 |
| --- | --- | --- | --- |
| `settings` | `{ firstMoveRule }` | 방장, 진행 중이 아닐 때 | `SETTINGS_CHANGED` |
| `start` | — | 방장, 두 명일 때 | `STARTED` |
| `place` | `{ to }` | 차례인 사람 | `STATE_CHANGED` / `FINISHED` / 거절 |
| `move` | `{ from, to }` | 차례인 사람 | 〃 |
| `remove` | `{ at }` | 3연속을 만든 사람 | 〃 |
| `resign` | — | 판의 두 사람 | `FINISHED` |
| `draw-offer` | — | 판의 두 사람 | `DRAW_OFFERED` (상대가 이미 제안했으면 `FINISHED`) |
| `draw-accept` | — | 제안받은 사람 | `FINISHED` |
| `draw-decline` | — | 제안받은 사람 | `DRAW_DECLINED` |
| `sync` | — | 방 사람 | 개인 큐로 `SNAPSHOT` |

지점 번호는 DTO 에서 0..23 으로 검증한다. 검증에 실패하면 `INVALID_REQUEST`.

### 서버 → 클라이언트

| 목적지 | 받는 사람 | 내용 |
| --- | --- | --- |
| `/topic/rooms/{roomId}` | **그 방 사람만** 구독 가능 | `RoomEvent` |
| `/topic/lobby` | 로그인한 누구나 | `{ type: LOBBY_CHANGED }` 신호만 |
| `/topic/system` | 로그인한 누구나. **지금 붙은 서버의 세션에게만** 감 | `{ type: RECONNECT }` — 이 서버가 곧 내려감. 새 연결을 먼저 붙이고 이 연결을 닫음 |
| `/user/queue/errors` | 나만 | 거절 사유 `{ code, message }` 또는 `ErrorResponse` |
| `/user/queue/sync` | 나만 | `SNAPSHOT` 이벤트 |

### 프레임 검사 (`config/ClientFrameGuard`)

| 프레임 | 규칙 | 어기면 |
| --- | --- | --- |
| CONNECT | 유효한 토큰 필수 | 연결 거부 |
| SUBSCRIBE | `/user/**`, `/topic/lobby`, `/topic/system`, 그리고 **자기가 속한 방의 `/topic/rooms/{번호}`** 만 | 프레임을 조용히 버림(예외를 던지면 끊김 → 재접속 → 재거부 루프) |
| SEND | `/app/**` 만 | 예외. SimpleBroker 는 `/topic` 으로 보낸 SEND 도 중계하므로 막지 않으면 상대 화면에 가짜 판을 띄울 수 있음 |

방 토픽은 번호로 **끝나야** 한다. `/topic/rooms/7/x` 같은 변형은 거부한다.

### `RoomEvent`

```json
{ "type": "STATE_CHANGED", "state": { ...GameState }, "actorId": null }
```

| type | state | actorId | 뜻 |
| --- | --- | --- | --- |
| `PLAYER_JOINED` | — | 들어온 사람 | 대기실 갱신 |
| `PLAYER_LEFT` | — | 나간 사람 | 대기실 갱신. 방장이 나갔으면 방이 사라짐 |
| `SETTINGS_CHANGED` | — | 방장 | 선공 방식 바뀜 |
| `STARTED` | ✓ | — | 새 판 |
| `STATE_CHANGED` | ✓ | — | 착수 결과 |
| `FINISHED` | ✓ | — | 판 종료 (`winnerId`, `loserId`, `endReason`) |
| `DRAW_OFFERED` | — | 제안한 사람 | 상대 화면에 수락 창 |
| `DRAW_DECLINED` | — | 거절한 사람 | 제안한 사람에게 알림 |
| `SNAPSHOT` | ✓ | — | 재접속 복구. 개인 큐로만 |
| `OPPONENT_DISCONNECTED` | ✓ (끝난 판) | — | 상대가 끊겨 기권 처리됨 |
| `LOBBY_CHANGED` | — | — | 로비 토픽 전용 |

### `GameState`

흑/백 기준이다. 방장이 흑이라는 전제를 두지 않는다.

| 필드 | 뜻 |
| --- | --- |
| `board` | 지점 0~23 의 `BLACK`/`WHITE`/`EMPTY` |
| `blackId`, `whiteId`, `currentTurnId` | 사용자 id |
| `blackInHand`, `whiteInHand`, `blackOnBoard`, `whiteOnBoard` | 손·판의 돌 수 |
| `blackPhase`, `whitePhase` | `PLACING`/`MOVING`/`FLYING` |
| `awaitingRemoval` | 차례인 사람이 제거해야 함 |
| `status` | `PLAYING`/`FINISHED` (`WAITING` 은 프론트 초기값일 뿐 서버가 보내지 않음) |
| `winnerId`, `loserId`, `endReason` | 끝났을 때. 무승부면 id 가 null |
| `turnRemainingMillis` | 차례인 쪽 남은 시간. **받은 순간부터** 센다(서버와 내 시계가 어긋나도 맞음) |
| `revision` | 순서 판정용 번호 ([02](02-room-lifecycle.md#revision--이벤트-순서)) |

## 에러 코드 (`ErrorCode`)

| code | HTTP | 화면 문구 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 로그인이 필요합니다. |
| `INVALID_REQUEST` | 400 | 요청 형식이 올바르지 않습니다. |
| `TOO_MANY_REQUESTS` | 429 | 요청이 너무 잦습니다. 잠시 후 다시 시도해 주세요. |
| `NOT_FOUND` / `USER_NOT_FOUND` | 404 | |
| `ROOM_NOT_FOUND` | 404 | 방을 찾을 수 없습니다. |
| `ROOM_FULL` | 400 | 방이 가득 찼습니다. |
| `ALREADY_JOINED` | 400 | 이미 참가한 방입니다. |
| `NOT_ROOM_HOST` | 403 | 방장만 할 수 있습니다. |
| `NOT_ENOUGH_PLAYERS` | 400 | 두 명이 모여야 시작할 수 있습니다. |
| `GAME_ALREADY_STARTED` | 400 | 이미 시작된 게임입니다. |
| `INTERNAL_ERROR` | 500 | 처리 중 오류가 발생했습니다. |

STOMP 에서 난 예외는 `@RestControllerAdvice` 가 닿지 않아 `GameStompController` 의 `@MessageExceptionHandler` 가 개인 큐로 보낸다.
규칙 위반 거절(`RejectReason`)은 예외가 아니라 결과값이고 `MoveRejectedResponse` 로 같은 큐에 간다.

## 프론트 쪽 위치

| 무엇 | 어디 |
| --- | --- |
| REST 호출 | `web/src/lib/api.ts` (axios, 401 이면 토큰 삭제) |
| 쿼리 키·캐시 | `web/src/lib/queries.ts` |
| 소켓 클라이언트·목적지·행동 이름 | `web/src/lib/socket.ts` |
| 판 상태·행동 전송 | `web/src/hooks/useGameState.ts` |
| 타입 | `web/src/lib/types.ts` (서버 응답과 같은 이름) |
