# 06. 바꿀 때 보는 곳

## 깨지면 안 되는 약속

| 약속 | 지키는 곳 | 어기면 |
| --- | --- | --- |
| 판이 바뀌는 길은 `MorrisGame.apply` 하나 (+ 서버가 부르는 `timeout`, `abandon`) | `MorrisGame` | 규칙 검증을 우회한 판 |
| 방이 바뀌는 길은 `RoomRegistry.mutate` 하나 | `RedisRoomRegistry` | 여러 서버가 같은 판을 읽고 각자 써서 수가 사라짐 |
| `mutate` 의 action 안에서 **DB·외부 호출·같은 방 `mutate` 금지** | `GameService`, `RoomService` | DB 를 기다리는 동안 그 방의 모든 프레임이 막힘. 같은 방 락을 다시 잡으면 멈춤 |
| 판이 끝나는 경로는 전부 `LockedResult` → `afterUnlock` 으로 정산 | `GameService` | 그 경로만 전적·MMR 이 빠짐 |
| 방을 지울 때는 `room.close()` 를 부르고 저장소가 지우게 함 | `Room.close`, `RedisRoomRegistry` | action 안에서 직접 지우면 끝에 다시 써져 방이 되살아남 |
| 요청자는 `Principal` → `actorId` 에서만 | 컨트롤러 | 남의 id 로 기권해 자기 점수를 올림 |
| 규칙 거절은 요청자 개인 큐로만 | `GameStompController` | 상대에게 내 실수가 보임 |
| 방 토픽 구독은 그 방 사람만 | `ClientFrameGuard` | 남의 판·양쪽 userId 를 실시간으로 봄 |
| revision 은 변경 적용 전에 올림 | `RedisRoomRegistry.mutate` | 다른 판이 같은 번호를 달아 화면이 한 수 전으로 돌아감 |
| visitor 판은 기록하지 않음 | `MatchResultService` | 무한 계정으로 배치·랭킹 오염 |
| 화면 문구에 "밀" 을 쓰지 않음 | 응답 문구, 프론트 | 사용자에게 전달되지 않는 용어 |

## 이럴 때 고칠 곳

### STOMP 행동을 하나 추가

1. `morris-core`: `Move` 에 종류 추가 → `MorrisGame.apply` 의 switch(컴파일러가 빠짐을 잡음) → `MoveResult` 필요하면 추가 → `MorrisGameTest`
2. `morris-api`: 요청 DTO(`game/dto/request`, 검증 어노테이션) → `GameStompController` 에 `@MessageMapping("/rooms/{roomId}/새행동")` → `GameService.applyMove` 의 결과 분기 → `GameServiceTest`
3. 새 이벤트가 필요하면 `RoomEventType` 추가
4. `web`: `lib/socket.ts` 의 `RoomAction` 유니온, `hooks/useGameState.ts` 에 행동, `lib/types.ts` 의 `RoomEventType`, `pages/Game/index.tsx` 의 이벤트 switch
5. 프로토콜 문서([03](03-protocol.md)) 표

`/app/rooms/{id}/...` 아래면 구독·전송 검사, 로그 컨텍스트(roomId)는 자동으로 적용된다.

### 방이나 판에 값을 하나 추가

판 상태는 **Redis 에 JSON 으로 저장**되고 롤링 배포 중에는 **옛 서버와 새 서버가 같은 JSON 을 읽는다.**

- `Room` 필드 → `RoomSnapshot` 에 추가 → `Room.snapshot()` / `Room(RoomSnapshot)` 양쪽
- 판 필드 → `GameSnapshot` 에 추가 → `MorrisGame.snapshot()` / `MorrisGame.restore()` 양쪽 → `GameSnapshotTest` 가 왕복을 확인
- 옛 JSON 에는 새 필드가 없다. 없을 때(null·0) 기본값으로 동작해야 한다. 옛 서버는 모르는 필드를 무시한다(Spring Boot 기본 `FAIL_ON_UNKNOWN_PROPERTIES=false`)
- 이름을 바꾸거나 의미를 바꾸면 배포 중 진행 중인 판이 깨진다. 새 필드로 추가하고 옛 필드는 다음 배포에 지운다
- **3회 반복 기록의 키 형식**(`MorrisGame.recordPosition`)을 바꾸면 진행 중인 판의 반복 집계가 끊긴다
- 로비에 보여야 하는 값이면 `Room.LobbyView` 에도 넣는다. 안 넣으면 바뀌어도 로비 신호가 안 나간다
- 화면에 보여야 하면 `GameStateResponse` / `RoomDetailResponse` / `RoomSummaryResponse` 와 `web/src/lib/types.ts`

### 판이 끝나는 새 경로

`MorrisGame` 에 `MoveResult.Finished` 를 돌려주는 메서드를 두고, `GameService` 에서 락 안에서 `Settlement.of(...)` 를 만들어
`LockedResult.settled(...)` 로 돌려준 뒤 `afterUnlock` 을 거친다. `exit`, `expireIfDue`, `purgeIfIdle` 이 예시다.

### DB 스키마

- **적용된 마이그레이션 파일은 절대 고치지 않는다.** 체크섬이 바뀌어 기동 때 Flyway 검증이 실패한다. 새 `V3__...sql` 을 추가한다
- 엔티티를 바꾸면 `ddl-auto: validate` 가 기동 때 어긋남을 잡는다. 테스트도 같은 검증을 탄다

### Redis 키·스크립트

- 키 이름은 [05](05-architecture.md#redis-키-전체) 표에 추가한다
- 스크립트는 고정 문자열로 두고 값은 `KEYS`/`ARGV` 로만 넘긴다(인젝션 방지)
- 스크립트가 여러 키를 건드리면 Redis Cluster 로 갈 때 해시 태그(`{room:7}`)가 필요하다
- 만료 없는 키는 정리 경로가 있어야 한다. 죽은 서버는 정리를 못 한다

### 레이팅 규칙

`morris-core` 의 `Rating` / `RatingPolicy` / `Tier` 를 고치고 `RatingPolicyTest` 등을 고친다.
반영 흐름은 `MatchResultService.record`. 티어 한글 표기는 `web/src/lib/tier.ts`.

## 테스트 위치

| 무엇 | 테스트 | 실행 환경 |
| --- | --- | --- |
| 규칙 | `morris-core` `MorrisGameTest`, `BoardTest`, `MillsTest`, `AdjacencyTest`, `GameSnapshotTest` | 순수 JUnit |
| 레이팅 계산 | `RatingPolicyTest` 등 | 순수 JUnit |
| 서비스 흐름 | `GameServiceTest` | 메모리 저장소(`InMemoryRoomRegistry`), 시계 조작(`MutableClock`) |
| 저장소 계약 | `RoomRegistryContract` (`RedisRoomRegistryTest`, `InMemoryRoomRegistryTest` 둘 다 통과해야 함) | Testcontainers Redis |
| 정산·랭킹 쿼리 | `MatchResultServiceTest`, `UserRepositoryTest`, `UserServiceTest` | Testcontainers MySQL |
| 다중 서버 | `MultiServerGameTest`, `ClusterBroadcasterTest`, `ClusterPresenceTest` | 같은 Redis 를 보는 서버 둘 |
| 소켓 전체 흐름·인가 | `GameE2eTest` | 실제 STOMP 클라이언트 |
| 인증 | `VisitorAuthTest`, `RedisOAuth2AuthorizationRequestRepositoryTest` | |

```bash
cd server && ./gradlew spotlessApply build      # 포맷 + 컴파일 + 전체 테스트 (Docker 필요)
cd web && pnpm exec tsc --noEmit && pnpm lint --fix && pnpm build
```

## 알려진 한계

고칠지 정하지 않은 것. 손대기 전에 여기와 `plans/` 를 본다.

| 무엇 | 영향 |
| --- | --- |
| 방 토픽 구독 권한을 **구독할 때만** 검사함 | 방에서 나간 뒤에도 같은 소켓이면 그 방의 다음 판 이벤트를 계속 받음 (관전 수준 정보) |
| 로그인 콜백이 프래그먼트의 토큰을 출처 확인 없이 받음 | 남이 만든 링크로 남의 계정에 로그인될 수 있음 (로그인 CSRF). 게임 계정이라 피해는 작음 |
| `forward-headers-strategy: framework` | 앱 포트에 직접 닿으면 `X-Forwarded-For` 위조로 visitor 발급 한도 우회. 프록시만 앱에 닿는다는 전제 |
| 정산 실패 자동 재시도 없음 | DB 장애 중 끝난 판은 ERROR 로그를 보고 수동 복구 |
| 반복 대전·10수 조건은 화면의 "랭크전" 표시에 반영되지 않음 | 끝나 봐야 랭크전이 아니었음을 앎 |
| `.platform/nginx.conf` | Elastic Beanstalk 시절 설정. 현재 쿠버네티스 배포에서는 쓰지 않음 |
| `application-local.yml` 은 git 이 추적하지 않음 | 로컬 프로파일 설정은 각자 기기에만 있음 |
