# 05. 구조와 운영

결정의 경위(왜 Redis 인가, 왜 RabbitMQ 가 아닌가 등)는 `plans/07-architecture-decision.md`(ADR), `plans/18-multi-server.md` 에 있다.
여기는 지금 구조를 고칠 때 필요한 사실만 모은다.

## 모듈

```
morris-api ──▶ morris-core
     │    └──▶ morris-storage ──┐
     └───────────────────────────┴──▶ morris-support
```

| 모듈 | 들어 있는 것 | 넣지 말 것 |
| --- | --- | --- |
| `morris-core` | 게임 규칙(`MorrisGame`, `Board`, `Mills`, `Adjacency`), 레이팅(`Rating`, `RatingPolicy`, `Tier`), 판 스냅샷 | Spring·JPA·STOMP. 규칙만 산다 |
| `morris-storage` | JPA 엔티티·리포지토리(`User`, `Match`), Redis 방 저장소(`RoomStore`), 방 스냅샷(`RoomSnapshot`), Flyway 마이그레이션 | 도메인 객체 `Room`. 저장소는 스냅샷(데이터)만 다룬다 |
| `morris-support` | 에러 코드·응답 모양(`ErrorCode`, `ErrorResponse`, `CustomException`), 로그 컨텍스트(`LogContext`) | |
| `morris-api` | 컨트롤러·서비스·보안·STOMP 설정·스케줄러·관측. 패키지 `auth`, `game`, `user`, `match`, `messaging`, `security`, `config`, `observability` | |

WebSocket 에 묶인 Redis 사용(방 이벤트 중계 `ClusterBroadcaster`, 접속 여부 `ClusterPresence`)은 저장이 아니라서 `morris-api/messaging` 에 있다.

### 계층 규칙

- 컨트롤러가 요청 DTO 를 `RoomCommand` 로 바꿔 서비스에 넘긴다. **Command 의 첫 인자는 서버가 확정한 `actorId`**
- 서비스는 `SimpMessagingTemplate` 을 모른다. 결과(`RoomEvent`, `RoomBroadcast`, `PlayOutcome`)를 돌려주고 전송은 컨트롤러·스케줄러가 한다
- 서비스는 `SecurityContextHolder` 를 읽지 않는다(STOMP 스레드에서는 비어 있다)

## 다중 서버

서버는 상태를 갖지 않는다. 아무 서버나 죽어도 클라이언트는 다른 서버로 다시 붙어 그대로 둔다.

| 상태 | 어디 | 담당 |
| --- | --- | --- |
| 방·판 | Redis | `RoomStore` ← `RedisRoomRegistry` |
| 방 이벤트 전달 | Redis pub/sub | `ClusterBroadcaster` |
| 사용자가 그 방에 붙어 있는가 | Redis | `ClusterPresence` |
| 이 서버의 소켓 ↔ 사용자·방 | 서버 메모리 | `SessionTracker` (끊긴 세션이 어느 방을 보던 것인지만 앎) |
| 전적·MMR | MySQL | JPA |

### 방 하나를 바꾸는 법 — `RoomRegistry.mutate`

**방에 대한 모든 변경은 `mutate` 하나로만** 일어난다. 방끼리는 병렬, 한 방 안에서는 한 번에 하나씩.

```
SET lock:room:{id} <토큰> NX PX 5000    (못 잡으면 백오프 + 지터로 재시도, 3초 넘으면 실패)
HGETALL room:{id}                       (json + ver)
  → Room.restore → revision++ → action(room) → 바뀐 게 있나?
EVAL save.lua                           (ver 확인 → json 쓰기 → 색인 3개 갱신 → 내 토큰이면 락 해제)
  또는 EVAL delete.lua                  (room.close() 를 불렀으면)
  또는 EVAL unlock.lua                  (바뀐 게 없으면 쓰지 않고 락만)
```

- **쓰기 때 버전 확인(CAS)**: GC 등으로 5초 넘게 멈췄다 깨어난 서버의 늦은 쓰기를 막는다. 락만으로는 못 막는다
- **Lua 로 묶는 이유**: 쓰기와 색인 갱신이 중간에 끊기면 방은 있는데 색인에 없는 상태가 생긴다. 왕복도 줄어든다
- **바뀐 게 없으면 쓰지 않는다**: 변경 전후 스냅샷을 비교한다(`RoomSnapshot`·`GameSnapshot` 은 값 record). revision·활동 시각도 오르지 않는다
- `find` / `findAll` 은 락 없이 읽는다. 변경 하나가 JSON 통째로 쓰이므로 사본에 변경 중간 상태가 섞이지 않는다
- 스크립트가 여러 키를 건드리므로 **Redis Cluster 가 아니라 단일 마스터(Sentinel)** 를 전제로 한다

### 방 이벤트 전달

- 서버마다 SimpleBroker 가 **자기에게 붙은 세션만** 안다. 그래서 방마다 Redis 채널(`morris:room:{id}`)을 두고,
  그 방을 구독한 세션이 이 서버에 생기면 채널을 구독하고 다 떠나면 해제한다(서버별 참조 카운트)
- 채널 하나로 모든 서버에 뿌리지 않는다. 서버가 8대면 두 사람의 판이 나머지 6대에도 가서 버려진다
- 보낸 서버도 채널을 거쳐 받는다. 자기 브로커로 직접 보내면 두 번 간다
- 로비(`morris:lobby`)는 드문 신호라 모든 서버가 늘 구독한다
- 받은 JSON 은 다시 파싱하지 않고 바이트째 브로커로 넘긴다
- **pub/sub 은 전달을 보장하지 않는다.** 클라이언트가 구독 직후와 재접속 때 `sync` 로 판 전체를 받아 메운다
- 개인 큐(`/user/queue/*`)는 이 경로를 타지 않는다. 그 프레임을 보낸 세션이 이 서버에 있다

### 접속 여부

- `presence:{roomId}:{userId}` 집합에 `"서버id:세션id"` 를 넣고 뺀다. 12시간 만료(들어올 때마다 갱신)
- 서버마다 `instance:{서버id}` 생존 키를 5초마다 15초 TTL 로 갱신한다. 생존 키가 없는 서버의 세션은 없는 것으로 본다(죽은 서버는 해제 이벤트를 못 보냄)
- 서버 id 는 `HOSTNAME`(쿠버네티스 파드 이름), 없으면 UUID

### Redis 키 전체

| 키 | 타입 | 내용 | 만료 |
| --- | --- | --- | --- |
| `room:{id}` | hash | `json`(RoomSnapshot), `ver`(쓰기 버전) | 없음 (삭제는 유휴 정리·방장 퇴장) |
| `rooms` | set | 방 id 목록 | |
| `rooms:turn-deadline` | zset | 진행 중인 방의 턴 마감(ms) — 시간 초과 후보 | |
| `rooms:activity` | zset | 마지막 활동(ms) — 유휴 정리 후보 | |
| `rooms:seq` | string | 방 번호 발급 | |
| `lock:room:{id}` | string | 방 락 토큰 | 5초 |
| `presence:{roomId}:{userId}` | set | 붙어 있는 세션 | 12시간 |
| `instance:{서버id}` | string | 서버 생존 | 15초 |
| `limit:visitor:{ip}` | zset | visitor 발급 시각 | 1분 |
| `oauth2:authorization-request:{state}` | string | 카카오 인가 요청 JSON | 5분 |
| 채널 `morris:room:{id}`, `morris:lobby` | pub/sub | 방 이벤트, 로비 신호 | |

로컬 compose 의 Redis 는 AOF 를 켜고 볼륨(`redis-data`)에 둔다. 재시작해도 진행 중인 판이 남는다.

## MySQL

스키마는 **Flyway** 가 만들고 Hibernate 는 `ddl-auto: validate` 로 검사만 한다. 테스트도 같은 마이그레이션으로 Testcontainers MySQL 을 띄운다.

| 테이블 | 내용 |
| --- | --- |
| `users` | `user_id`, `provider`(KAKAO/VISITOR), `provider_id`, `nickname`, `image_url`, `role`, `mmr`, `peak_mmr`, `wins`, `losses`, `draws`, `created_at` |
| `matches` | `black_id`, `white_id`, `winner_id`(무승부면 null), `end_reason`, `move_count`, `rated`, 양쪽 `mmr_before`·`mmr_delta`, `finished_at` |

| 인덱스 | 쓰는 곳 |
| --- | --- |
| `uk_users_provider (provider, provider_id)` | 로그인 조회, 같은 카카오 계정 중복 방지 |
| `idx_users_provider_mmr (provider, mmr desc)` | 랭킹 상위 N, 내 등수 |
| `idx_users_provider_created_at (provider, created_at)` | visitor 정리 |
| `idx_matches_black/white (…_id, finished_at)` | 반복 대전 감쇠 (같은 상대·최근 24시간) |

- 외래키는 DB 에 걸지 않는다. 참조 무결성은 애플리케이션이 지킨다
- 연관관계는 `@ManyToOne(LAZY)` 만 쓴다

## 스케줄러

| 작업 | 주기 | 하는 일 |
| --- | --- | --- |
| `TurnTimeoutScheduler` | 1초 | 마감 지난 방을 락 안에서 다시 확인하고 차례인 쪽 패배 처리 |
| `IdleRoomPurger` | 5분 | 30분 동안 변화 없는 방 삭제. 진행 중이면 무승부(`ABANDONED`) 정산 |
| `ClusterPresence.heartbeat` | 5초 | 이 서버의 생존 키 갱신 |
| `IdleVisitorPurger` | 매일 04:00 | 만든 지 30일 지난 visitor 삭제 |
| 재접속 유예 | 끊김 후 10초 1회 | 다른 서버로 다시 붙었는지 보고 아니면 이탈 처리 |

- 시간 초과·유휴 정리는 **여러 서버가 같은 방을 동시에 골라도 안전**하다. 후보는 락 없이 고르고, 락 안에서 다시 확인해 두 번째 서버는 아무것도 하지 않는다
- 방 하나의 실패가 같은 틱의 다른 방을 막지 않는다(방 단위로 예외를 가둠)
- 스케줄러가 STOMP 하트비트와 같은 스케줄러를 쓴다(`SchedulingConfig`). 스케줄러가 죽으면 판이 끝나지 않으므로 헬스체크(`GameSessionHealthIndicator`)가 이것 하나로 DOWN 을 판단한다

## 연결

| 항목 | 값 | 이유 |
| --- | --- | --- |
| STOMP 하트비트 | 10초 (서버 기준, 협상은 큰 값) | 없으면 FIN 없이 끊긴 소켓을 몇 분간 살아 있다고 봄. 3배(30초) 동안 아무것도 안 오면 닫음 |
| 재연결 | 0.5초부터 두 배씩 5초까지 (`ManagedSocket`) | 서버 교체처럼 곧 붙을 끊김은 바로 이어지고, 길어지면 덜 두드림 |
| 끊김 창 | 3초 넘게 못 붙을 때만 | 짧은 끊김은 화면에 보이지 않게 |
| 종료 전 세션 이동 대기 | 최대 10초 (`SocketDrainer`) | `RECONNECT` 를 보내고 세션이 다른 서버로 옮길 시간. 쿠버네티스 종료 유예(70초) 안 |
| 재접속 유예 | 10초 | 새로고침·화면 이동으로 방이 사라지지 않게 |
| 방 락 임대 / 대기 | 5초 / 3초 | |

## 설정 (환경변수)

| 변수 | 용도 |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `local`(호스트 직접 실행) / `k8s`(JSON 로그, SQL 로그 끔). 이미지에 박지 않는다 |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | MySQL. 연결에 `connectionTimeZone=UTC` 필수 |
| `MYSQL_*` | compose 가 띄우는 MySQL 계정·DB |
| `JWT_SECRET_KEY` | HS256 키, Base64 32바이트 이상 |
| `ACCESS_TOKEN_EXPIRATION` / `VISITOR_TOKEN_EXPIRATION` | ms. 기본 1시간 / 7일 |
| `KAKAO_CLIENT_ID` / `KAKAO_CLIENT_SECRET` | 카카오 OAuth |
| `DOMAIN` | 로그인 성공 후 돌려보낼 프론트 주소 |
| `CORS_ALLOWED_ORIGINS` | 프론트 오리진 목록. 와일드카드 없음 |
| `HOSTNAME` | 서버 id (쿠버네티스가 넣어 줌) |
| `game.turn-time-limit` / `game.room.idle-timeout` | 기본 60초 / 30분 |

프론트 빌드 변수: `VITE_API_URL`, `VITE_SOCKET_URL`, `VITE_KAKAO_URL`. 배포 경로는 `/nine-mens-morris/`(개발 서버는 `/`).

## 관측

- `/actuator/health`(liveness·readiness), `/actuator/prometheus` 만 노출. 인그레스에 라우팅하지 않는다
- 게임 메트릭(`GameMetrics`): `morris.rooms.active`(우상향이면 정리 누수), `morris.sockets.open`, `morris.games.finished{reason}`, `morris.moves.rejected{reason}`(오르면 규칙 불일치나 조작 시도), `morris.room.wait`(상대를 만나 첫 판 시작까지)
- 모든 로그에 `traceId` / `userId` / `roomId` (HTTP 는 `MdcRequestFilter`, STOMP 는 `StompLogContextInterceptor`, 스케줄러는 `SchedulerLogContextConfig`). 실패 응답에도 같은 `traceId` 가 실린다
- 로컬 관측 스택: `server/observability/` (Prometheus + Loki + Grafana + Alloy)

## 배포

| 대상 | 어떻게 |
| --- | --- |
| 서버 | `main` push → `server.yml`: 빌드·테스트 → arm64 이미지를 GHCR 에 sha 태그로 → 매니페스트 리포 태그 갱신 → ArgoCD 가 쿠버네티스에 배포 |
| 웹 | `web.yml`: 빌드 → GitHub Pages (배포는 수동 실행) |
| 로컬 | `server/docker-compose.yml` (`database`, `redis`, `--profile app` 이면 서버까지) |

이미지는 non-root 사용자로 실행한다. 이미지 안에서는 테스트를 돌리지 않는다(Testcontainers 가 Docker 를 요구). CI 가 돌린다.
