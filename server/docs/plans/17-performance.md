# 17. 성능 점검과 개선 (2026-10)

요청마다 도는 경로부터 거꾸로 따라가며 **측정 → 원인 → 변경 → 다시 측정** 순서로 정리했다.
추측으로 고친 것은 없다. 측정해 보니 가설이 틀린 경우도 그대로 적었다.

---

## 요약

| # | 대상 | 문제 | 결과 | 커밋 |
| --- | --- | --- | --- | --- |
| 1 | 인증 | 모든 요청이 역할을 읽으려고 `users` 를 PK 조회 | 요청당 쿼리 **1 → 0** | `511f574` |
| 2 | 랭킹 상위 100 | 회원 전체를 읽고 정렬 | **12.3ms → 0.14ms** (회원 2만) | `38d9002` |
| 2 | 내 등수 | 회원 행을 전부 읽고 mmr 필터 | **10.2ms → 2.3ms** | `38d9002` |
| 2 | 게스트 정리(매일) | 게스트 전체를 읽고 날짜 필터 | **82ms → 0.56ms** (게스트 20만) | `38d9002` |
| 3 | 정산 | 동시에 끝난 두 판 중 하나가 사라짐 (lost update) | 재현 20승 중 10승 → **20승**. 조회 2 → 1 | `886beb7` |
| 4 | STOMP | 하트비트가 협상 결과 0 으로 꺼져 있음 | 끊긴 소켓 정리 **수 분 → 30초 이내** | `2203df1` |
| 7 | 웹 첫 접속 | 효과음 11개·한글 폰트 전체를 메인에서 받음 | **약 2.4MB → 0.3MB** | `b8b303d` `8b6b11e` |

3·4 는 속도보다 정확성·자원 문제지만 같은 점검에서 나와 함께 적는다.

---

## 1. 인증에서 요청마다 하던 사용자 조회

### 무엇이 문제였나

`TokenAuthenticator` 가 토큰을 검증한 뒤 역할(`ROLE_USER` / `ROLE_VISITOR`)을 얻으려고
`userRepository.findById` 를 호출했다. HTTP 필터와 STOMP `CONNECT` 가 모두 이 경로를 지난다.

이게 특히 아픈 곳은 **로비**다. 방이 생기거나 사라지면 서버가 `/topic/lobby` 로 신호만 보내고,
로비에 있는 클라이언트 M명이 **동시에** `GET /rooms` 를 보낸다(목록을 소켓으로 M번 복제 전송하지 않으려는 설계).
그 M개 요청 각각에 인증 쿼리가 하나씩 붙어 있었다.

토큰 파싱도 `validateToken` 과 `extractSubject` 에서 **두 번** 하고 있었다.

### 왜 DB 를 안 봐도 되나

| 확인한 것 | 결과 |
| --- | --- |
| 역할이 바뀌는 경로 | 없음. 회원/게스트는 가입 경로(`Provider`)로 정해지고 한 userId 에서 바뀌지 않음 |
| 토큰이 살아 있는 동안 계정이 지워지는 경로 | 없음. 게스트 정리는 생성 **30일** 뒤, 게스트 토큰 수명은 **7일**. 회원 탈퇴 기능 없음 |

존재 확인이 막아 주던 것은 "로컬 DB 를 초기화한 경우" 뿐이었다.

### 변경

- 발급할 때 `role` 클레임을 싣고, 인증은 서명된 토큰만 보고 끝냄
- 파싱을 한 번으로 합침 (`JwtProvider.parse` → `TokenClaims(userId, role)`)
- **클레임이 없는 배포 전 토큰**은 기존처럼 DB 로 확인. 그런 토큰이 모두 만료되면(최대 7일) 이 분기를 지워도 됨

### 검증

`AuthenticationQueryCountTest` — Hibernate `Statistics` 로 셈.
없는 방을 조회하는 요청(인증 통과 후 메모리만 보고 404)의 prepared statement 수를 본다.

| | 변경 전 | 변경 후 |
| --- | --- | --- |
| 인증된 요청 1건의 인증 쿼리 | 1 | **0** |

### 회원 탈퇴가 생기면

토큰만 보고 통과시키므로 탈퇴한 사용자의 토큰이 만료 전까지 유효하다.
그때는 토큰 무효화 목록(ADR-6 의 Redis 용도 중 하나)이 필요하다. 지금 다시 DB 조회로 돌아가는 것은 답이 아니다.

---

## 2. 랭킹·내 등수·게스트 정리 인덱스 (Flyway V2)

### 측정 조건

- 로컬 Docker `mysql:8.4`, Apple Silicon. 사용자 개발 DB 와 분리한 임시 스키마(`perf_bench`)
- `EXPLAIN ANALYZE` 를 두 번 돌려 **두 번째 값**(버퍼 풀이 데워진 상태)을 씀
- 한 번 측정한 값이라 자릿수 비교로만 읽을 것. 재현 SQL 은 6절

데이터 분포가 핵심이다. **게스트는 MMR 이 움직이지 않아 전원 1000** 이고, 회원보다 훨씬 많이 쌓인다.

### 처음 가설은 틀렸다

"게스트가 MMR 1000 에 몰려 있어 `idx_users_mmr` 를 타면 그 구간의 게스트 행을 일일이 읽을 것" 이라고 봤다.
실제로는 옵티마이저가 `provider <> 'VISITOR'` 를 유니크 키 `(provider, provider_id)` 의 두 범위로 풀어
**회원 행만** 읽고 있었다. 게스트 2만 / 회원 500 에서는 문제가 없었다.

| 질의 | 게스트 2만 / 회원 500 |
| --- | --- |
| 랭킹 상위 100 | 1.98ms — 회원 500 전부 읽고 정렬 |
| 내 등수 | 11.7ms — 회원 500 행을 읽고 mmr 필터 (첫 실행이라 차가운 값) |
| 게스트 정리 | 7.6ms — 게스트 2만 행을 읽고 날짜 필터 |

문제는 **비용이 전체 회원 수·게스트 수에 비례**한다는 것이다. 규모를 키워 다시 쟀다.

### 게스트 20만 / 회원 2만

| 질의 | V1 | V2 | 계획 변화 |
| --- | --- | --- | --- |
| 랭킹 상위 100 | 12.3ms | **0.14ms** | 회원 2만 읽고 정렬 → 인덱스 순서대로 **100개만** 읽고 멈춤 |
| 내 등수 | 10.2ms | **2.3ms** | 회원 행 2만 조회 → 커버링 인덱스 범위 스캔 (행 접근 없음) |
| 게스트 정리 | 82ms | **0.56ms** | 게스트 20만 행 조회 → 커버링 범위 스캔 3,403건 |

게스트 정리는 **매일 도는 실제 상황**으로 쟀다. 정리가 매일 돌면 기준(30일)을 넘긴 행은 하루치뿐이다.
처음엔 60일치가 정리 없이 쌓인 데이터로 재서 절반이 대상이었고, 그때는 옵티마이저가 새 인덱스를 고르지 않았다.

### 변경

```sql
alter table users
    drop index idx_users_mmr,
    add index idx_users_provider_mmr (provider, mmr desc),
    add index idx_users_provider_created_at (provider, created_at);
```

- **`mmr DESC`**: 정렬이 `mmr desc, user_id asc` 다. InnoDB 보조 인덱스 뒤에는 PK 가 오름차순으로 붙으므로
  `(provider, mmr desc)` 의 저장 순서가 곧 정렬 순서다. `mmr asc` 로 만들면 역방향 스캔에서 `user_id` 도 내림차순이 되어 정렬이 다시 생긴다
- **`idx_users_mmr` 제거**: `provider` 조건이 항상 같이 붙어 쓰이지 않았다

### 질의는 하나만 바꿨다

| 질의 | 형태 | 이유 |
| --- | --- | --- |
| 랭킹 | `provider <> VISITOR` → **`provider in (:members)`** | `<>` 는 범위가 둘로 갈라져 인덱스 순서를 못 쓴다. 회원 제공자가 KAKAO 하나인 동안 `IN` 은 등호가 된다 |
| 내 등수 | **그대로** `<>` | 범위 집계라 순서가 필요 없다. `<>` 로도 새 인덱스를 커버링으로 탄다(2.3ms). `IN` 으로 바꾸면 옵티마이저가 유니크 키를 골라 **8.5ms 로 오히려 느려졌다** |
| 게스트 정리 | 그대로 | 이미 `provider = VISITOR` 등호 |

`:members` 는 `EnumSet.complementOf(VISITOR)` 로 만든다. 제공자를 질의에 `KAKAO` 로 박으면
구글 로그인이 생겼을 때 그 회원들이 **조용히 랭킹에서 빠진다.** 집합으로 두면 제공자가 늘어도 결과는 같고
정렬로 돌아갈 뿐이다.

### 검증

- Testcontainers MySQL 이 V1 → V2 를 적용하고 `ddl-auto: validate` 로 엔티티와 대조
- 랭킹 의미(게스트 제외, 동점 순서)는 기존 `UserRepositoryTest` 가 그대로 통과

---

## 3. 정산 lost update

### 무엇이 문제였나

`MatchResultService.record` 는 두 사람을 잠금 없이 읽고, MMR 과 승패를 계산해 덮어쓴다.
한 사람이 **여러 방에 속할 수 있어** 두 판이 같은 순간 끝나면 두 정산이 같은 행을 동시에 고친다.
둘 다 같은 MMR 을 읽고 각자 덮어써 한쪽 결과가 사라진다.

정산은 방 락 밖에서 돌기 때문에(방 락 안에서 DB 를 기다리지 않으려고) 방 락이 이것을 막아 주지도 않는다.

### 재현

`MatchResultServiceTest.동시_정산` — 10라운드 동안 같은 사람이 두 상대를 **같은 순간** 이기게 함.

| | 기대 | 변경 전 | 변경 후 |
| --- | --- | --- | --- |
| 승수 | 20 | **10** | 20 |
| MMR = 1000 + 원장 변동 합 | 일치 | 불일치 | 일치 |

### 변경

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select u from User u where u.userId in :userIds order by u.userId")
List<User> findAllByIdForUpdate(Collection<Long> userIds);
```

- `SELECT ... FOR UPDATE` 로 두 사람 행을 잠그고 읽음
- **항상 user_id 순으로** 잠근다. A-B 판과 B-A 판이 서로 반대 순서로 잠그면 교착이 난다
- `findById` 두 번이 한 번이 됨

낙관적 락(`@Version`)도 검토했다. 충돌하면 재시도 로직이 필요하고, 정산은 한 판에 한 번이라 경합이 드물어
잠금 대기 비용이 거의 없다. 비관적 락이 더 단순하다.

---

## 4. STOMP 하트비트

### 무엇이 문제였나

`enableSimpleBroker` 에 하트비트 값도 스케줄러도 주지 않아 **서버 쪽 하트비트가 0** 이었다.
STOMP 하트비트는 양쪽 값을 협상하는데 한쪽이 0 이면 그 방향은 꺼진다.
게임 화면은 4초를 요청하고 있었지만 실제로는 주고받지 않았다.

모바일 네트워크가 FIN 없이 끊기면 서버는 OS 의 TCP 타임아웃(수 분)까지 소켓이 살아 있다고 본다.

- 그동안 "상대 연결 끊김 → 기권 처리" 가 돌지 않아 남은 사람의 판이 멈춤
- 죽은 세션이 구독 정보와 버퍼를 잡고 있음

### 변경

- 서버 하트비트 10초, 스케줄러는 지연 주입(브로커 설정이 스케줄러를 만드는 쪽이라 순환 방지)
- Spring SimpleBroker 는 받은 프레임 없이 간격의 3배가 지나면 세션을 닫는다 → **30초 안에** 이탈 처리로 이어짐
- 협상은 큰 값으로 정해지므로 클라이언트의 4초 요청은 10초가 된다. 모바일 배터리에도 낫다

### 검증

`WebSocketHeartbeatTest` — 브로커 핸들러의 하트비트 값과 스케줄러를 확인.
이 설정은 빠뜨려도 아무 오류가 나지 않는 종류라 값을 테스트로 고정했다.

---

## 5. 검토했지만 하지 않은 것

| 후보 | 하지 않은 이유 |
| --- | --- |
| 로비 목록 캐시 (방 레지스트리 버전으로 무효화) | 1번으로 `GET /rooms` 는 이미 PK 묶음 조회 1개. 캐시를 두면 판이 끝난 직후 방장 MMR 이 DB 반영 전 값으로 남는 틈이 생김(정산이 방 락 밖에서 돌아 버전 증가와 커밋 사이가 벌어짐). 얻는 것보다 정합성 비용이 큼 |
| 로비 목록을 소켓으로 직접 전송 | 접속자 수만큼 같은 목록을 복제 전송. 지금 설계(신호 + REST)를 유지 |
| 가상 스레드 (`spring.threads.virtual.enabled`) | 방 락이 `synchronized` 라 Java 21 에서 캐리어 스레드를 고정함. 락 안에서 I/O 는 없어 실제 피해는 작지만, 측정 근거 없이 바꿀 이유가 없음. 동시 접속 부하 측정 후 판단 |
| `matches` 인덱스 | 반복 대전 감쇠 질의가 `(black_id, finished_at)` / `(white_id, finished_at)` 을 이미 탐. 한 판에 한 번이라 핫 경로 아님 |
| 착수 경로 | 방 단위 `synchronized` + 메모리 연산 + 브로드캐스트. DB 접근 없음. 착수마다 INFO 로그도 없음(게임 시작에만 있음) |
| HTTP 응답 압축 | 응답이 대부분 1KB 미만 |

---

## 6. 재현 방법

로컬 MySQL 컨테이너에 임시 스키마를 만들어 잰다. 개발 DB 는 건드리지 않는다.

```sql
create database perf_bench;
-- V1__init.sql 의 users 정의를 그대로 실행한 뒤

set session cte_max_recursion_depth = 300000;
insert into users(provider,provider_id,nickname,image_url,role,mmr,peak_mmr,wins,losses,draws,created_at)
with recursive seq(n) as (select 1 union all select n+1 from seq where n < 200000)
select 'VISITOR', null, concat('g',n), null, 'ROLE_VISITOR', 1000, 1000, 0,0,0,
       now(6) - interval floor(rand()*60*24*60) minute from seq;
insert into users(provider,provider_id,nickname,image_url,role,mmr,peak_mmr,wins,losses,draws,created_at)
with recursive seq(n) as (select 1 union all select n+1 from seq where n < 20000)
select 'KAKAO', concat('k',n), concat('m',n), null, 'ROLE_USER', 600 + floor(rand()*1000), 1000, 0,0,0, now(6) from seq;
analyze table users;

-- 랭킹 (V1 형태 / V2 형태)
explain analyze select * from users where provider <> 'VISITOR' order by mmr desc, user_id asc limit 100;
explain analyze select * from users where provider in ('KAKAO') order by mmr desc, user_id asc limit 100;
-- 내 등수
explain analyze select count(*) + 1 from users where mmr > 950 and provider <> 'VISITOR';
-- 매일 정리에서 걸리는 양 (60일 중 마지막 하루치)
explain analyze select count(*) from users where provider = 'VISITOR' and created_at < now(6) - interval 59 day;

-- V2 인덱스 적용 후 같은 질의를 다시 실행
drop database perf_bench;
```

---

## 7. 프론트엔드 첫 접속

서버 문서지만 같은 점검에서 나온 것이라 함께 적는다. `web/` 의 커밋이다.

### 측정 방법

- 프로덕션 빌드를 `vite preview` 로 띄우고 헤드리스 Chromium 의 CDP `Network` 이벤트로 받은 바이트(`encodedDataLength`)를 셈
- 캐시를 끄고 메인 화면(비로그인), `document.fonts.ready` 까지 기다림
- **개발 기기에 Pretendard 가 설치돼 있어** CSS 의 `local()` 이 먼저 잡혀 폰트를 받지 않았다.
  설치하지 않은 사용자를 흉내 내려고 Pretendard CSS 에서 `local()` 을 걷어내 응답함
  (이렇게 가로챈 CSS 는 압축 없이 다시 보내져 크기가 부풀어 보이므로 그 한 파일은 `curl` 의 br 전송량으로 대신함)

`vite preview` 자체가 깨져 있었다. `base` 를 `command === 'build'` 일 때만 주는데 preview 는 `serve` 로 들어와
빌드된 자산이 전부 404 였다 (`05a7fb5`).

### 결과 (폰트 미설치 사용자 기준)

| | 변경 전 | 변경 후 | 커밋 |
| --- | --- | --- | --- |
| 효과음 | 675KB (11개) | **0** — 게임 화면에 들어갈 때 받음 | `b8b303d` |
| Pretendard | 1,539KB (Light·SemiBold 한글 전체) | **약 120KB** (dynamic-subset CSS 21KB + 글자 조각) | `8b6b11e` |
| JS·CSS·기타 | 약 170KB | 약 170KB | |
| **합계** | **약 2.4MB** | **약 0.3MB** | |

- 효과음: `new Audio(src)` 는 만드는 순간 받기 시작한다. 모듈 최상단에서 11개를 만들어 메인만 열어도 전부 받았다
- Pretendard: `static` 판은 굵기마다 한글 전체(약 760KB)다. `dynamic-subset` 은 unicode-range 조각으로 나뉘어 화면에 나온 글자가 든 조각만 받는다
- 게임 외 화면 지연 로딩(`081e50a`)으로 첫 JS 487KB → 417KB (gzip 155KB → 134KB)

### 하지 않은 것

| 후보 | 이유 |
| --- | --- |
| axios → fetch | gzip 약 14KB. 토큰 첨부·401 처리 인터셉터를 다시 짜야 해 얻는 것에 비해 바뀌는 것이 많음 |
| Google Fonts 굵기 범위 축소 | 가변 폰트라 범위를 줄여도 파일이 하나. 메인에서는 제목 폰트(29KB)만 받음 |

---

## 8. 다음에 잴 것

- **동시 접속 부하**: 방 N개에서 동시에 착수할 때 STOMP 인바운드 채널 스레드풀(기본 코어 수 × 2)과
  방 락 경합. k6 의 WebSocket 시나리오나 Gatling 으로 p99 착수 왕복 시간을 잼
- 그 결과를 보고 가상 스레드, 인바운드 채널 풀 크기를 결정
