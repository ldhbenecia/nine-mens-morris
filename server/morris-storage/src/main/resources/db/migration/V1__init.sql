-- 스키마 최초 정의
--
-- 2024년부터 ddl-auto 가 스키마를 만들어 왔고 운영 DDL 은 어디에도 기록돼 있지 않았다
-- 구조가 확정된 지금 시점을 V1 으로 잡는다. 이후로는 운영도 테스트도 이 파일 위에서 돈다
--
-- 외래키는 걸지 않는다. 참조 무결성은 애플리케이션에서 지킨다

-- 식별자는 서비스가 발급한다
-- 카카오 회원번호를 기본키로 쓰면 회원번호가 없는 비로그인 사용자를 만들 수 없다
create table users
(
    user_id     bigint       not null auto_increment,
    provider    varchar(20)  not null,
    -- 카카오 회원번호. 비로그인 사용자는 null
    provider_id varchar(64)  null,
    nickname    varchar(20)  not null,
    image_url   varchar(500) null,
    role        varchar(20)  not null,
    mmr         int          not null,
    peak_mmr    int          not null,
    wins        int          not null,
    losses      int          not null,
    draws       int          not null,
    created_at  datetime(6)  not null,
    primary key (user_id),
    -- 같은 카카오 계정이 두 행이 되는 것을 막는다
    -- MySQL 은 유니크 인덱스에서 null 을 서로 다른 값으로 보므로
    -- provider_id 가 없는 비로그인 사용자 행은 여기 걸리지 않는다
    unique key uk_users_provider (provider, provider_id),
    -- 랭킹 정렬과 내 등수 계산이 여기에 걸린다. 없으면 매번 풀스캔
    key idx_users_mmr (mmr)
) engine = InnoDB
  default charset = utf8mb4
  collate = utf8mb4_0900_ai_ci;

-- 경기 원장
-- users 의 집계값(mmr, 승패무)은 언제든 이 표로 재계산할 수 있어야 한다
-- 승자/패자가 아니라 흑/백으로 저장해야 무승부를 표현할 수 있고,
-- 변동 전 MMR 을 남겨야 K 계수를 바꿔도 과거 경기를 다시 계산할 수 있다
create table matches
(
    id               bigint      not null auto_increment,
    black_id         bigint      not null,
    white_id         bigint      not null,
    winner_id        bigint      null,
    end_reason       varchar(30) not null,
    move_count       int         not null,
    rated            bit(1)      not null,
    black_mmr_before int         not null,
    white_mmr_before int         not null,
    black_mmr_delta  int         not null,
    white_mmr_delta  int         not null,
    finished_at      datetime(6) not null,
    primary key (id),
    -- 반복 대전 감쇠가 (상대, 최근 24시간) 으로 세므로 시각을 함께 묶는다
    key idx_matches_black (black_id, finished_at),
    key idx_matches_white (white_id, finished_at)
) engine = InnoDB
  default charset = utf8mb4
  collate = utf8mb4_0900_ai_ci;
