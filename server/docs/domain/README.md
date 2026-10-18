# 도메인 문서

나인멘스모리스 온라인 대전을 **처음 보는 개발자**가 이 시스템을 고치기 전에 알아야 할 것을 모은 곳이다.
`plans/` 가 "왜 이렇게 결정했는가(감사·ADR·설계 경위)" 라면, 여기는 **"지금 어떻게 돌아가는가"** 다.

코드가 바뀌면 이 문서도 같이 고친다. 문서와 코드가 다르면 코드가 맞고, 문서를 고칠 일이다.

## 읽는 순서

| 순서 | 문서 | 이걸 읽으면 |
| --- | --- | --- |
| 1 | [01-game-rules.md](01-game-rules.md) | 판·단계·3연속·제거·승패·무승부 규칙과 그 규칙이 코드 어디에 있는지 |
| 2 | [02-room-lifecycle.md](02-room-lifecycle.md) | 방이 생겨서 사라질 때까지 무슨 일이 일어나고 무엇이 브로드캐스트되는지 |
| 3 | [03-protocol.md](03-protocol.md) | REST·STOMP 엔드포인트, 이벤트, 에러 코드. 프론트와 서버의 계약 |
| 4 | [04-identity-and-rating.md](04-identity-and-rating.md) | 카카오 회원·비로그인(visitor), 토큰, MMR·티어·랭킹·정산 |
| 5 | [05-architecture.md](05-architecture.md) | 모듈 구조, 다중 서버(Redis 상태·락·pub/sub·접속 여부), DB, 스케줄러, 설정 |
| 6 | [06-change-guide.md](06-change-guide.md) | 기능을 추가·변경할 때 같이 고쳐야 하는 곳, 깨지기 쉬운 불변식, 알려진 한계 |

## 한 장 요약

```
 브라우저(React, GitHub Pages)
   │  REST  /api/v1/**        ← 로비·방 생성/입장/퇴장·내 정보·랭킹
   │  STOMP /ws               ← 판 진행(/app/rooms/{id}/...) · 방 이벤트 구독(/topic/rooms/{id})
   ▼
 서버 N 대 (Spring Boot, 상태 없음)
   │  방·판 상태 ──────────▶ Redis  (room:{id} JSON, 방 락, 색인)
   │  방 이벤트 중계 ───────▶ Redis pub/sub (morris:room:{id}, morris:lobby)
   │  접속 여부 ───────────▶ Redis  (presence, 서버 생존 키)
   │  전적·MMR ────────────▶ MySQL  (users, matches)
   ▼
 morris-core : 게임 규칙만 있는 순수 Java. 서버는 규칙을 여기에 위임만 함
```

- **규칙 판단은 전부 서버**(`MorrisGame`)가 한다. 프론트는 화면 편의로 일부를 미리 막을 뿐 신뢰 대상이 아니다
- **요청자는 항상 서버가 확정**한다(JWT → `Principal`). 요청 본문에 사용자 id 를 받지 않는다
- **판은 어느 서버에도 머물지 않는다.** 수마다 Redis 에서 읽고, 규칙을 적용하고, 다시 쓴다. 아무 서버나 죽어도 판이 이어진다
- 판이 끝나면 **락을 푼 뒤** MySQL 에 전적·MMR 을 반영한다

## 용어집

코드 식별자는 이 표를 따른다. 화면 문구는 오른쪽 열을 쓴다.

| 개념 | 코드 | 화면 | 메모 |
| --- | --- | --- | --- |
| 같은 색 돌 3개가 이룬 줄 | `mill` | 3연속 | "밀" 은 사용자에게 보이지 않는다 |
| 판 위의 자리 0~23 | `point` | 지점 | |
| 돌 | `stone` (`BLACK`/`WHITE`) | 돌 | |
| 인접 | `adjacent` / `Adjacency` | | |
| 1단계 돌 놓기 | `PLACING` | Phase 1 · 돌 배치 단계 | |
| 2단계 인접 이동 | `MOVING` | Phase 2 · 돌 이동 단계 | |
| 3단계 자유 이동 | `FLYING` | Phase 3 · 자유 이동 단계 | 돌이 정확히 3개일 때 |
| 기권 | `resign` | 기권 | |
| 무승부 | `draw` | 무승부 | |
| 방 | `room` / `roomId` | 방 | 판(`game`)은 방 안에 있다 |
| 방장 / 참가자 | `host` / `guest` | 방장 / 참가자 | 흑백과는 별개 |
| 로그인하지 않은 사용자 | `visitor` | 게스트 | `guest` 는 참가자라 쓰지 않는다 |
| 판 상태 번호 | `revision` | | 이벤트 순서 판정용 |
| 랭크전 | `rated` | 랭크전 | 두 자리가 모두 회원이고 10수 이상 |
