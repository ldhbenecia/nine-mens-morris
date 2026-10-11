-- 랭킹·내 등수·게스트 정리가 회원 수, 게스트 수에 비례해 느려지던 것
--
-- 랭킹 상위 N명: (provider, mmr DESC) 를 순서대로 N개만 읽고 멈춤
--   mmr 내림차순 + 동점은 user_id 오름차순이 이 인덱스의 저장 순서와 같아(PK 가 뒤에 오름차순으로 붙음) 정렬이 없음
--   provider 가 등호 조건일 때만 이 순서를 쓸 수 있음
-- 내 등수: 같은 인덱스로 테이블 행을 읽지 않고 셈
-- 게스트 정리: (provider, created_at) 범위로 기준일 이전 행만 찾음
--
-- idx_users_mmr 는 provider 조건이 같이 붙는 이 질의들에서 쓰이지 않아 대체함
alter table users
    drop index idx_users_mmr,
    add index idx_users_provider_mmr (provider, mmr desc),
    add index idx_users_provider_created_at (provider, created_at);
