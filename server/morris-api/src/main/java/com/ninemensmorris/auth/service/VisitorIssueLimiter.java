package com.ninemensmorris.auth.service;

import com.ninemensmorris.common.exception.CustomException;
import com.ninemensmorris.common.response.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

// 비로그인 계정 발급 한도. 인증 없이 부르는 쓰기 엔드포인트라 막지 않으면 users 행이 무한히 생김
//
// 짧은 시간에 몰아치는 발급만 막음. 그보다 느린 발급은 쌓여도 정리 배치가 놀고 있는 계정을 지움
// 시간 단위로 묶으면 새로고침·탭 여러 개처럼 평범한 사용도 막혀 버림
//
// 슬라이딩 창 — "지금부터 1분 전까지" 의 발급 수로 판단함
// 고정 창은 경계 앞뒤 몇 초 사이에 한도의 두 배가 통과함
//
// 카운터는 Redis 에. 서버마다 메모리에 두면 서버가 8대일 때 한도가 8배가 됨
// 판단과 기록을 한 Lua 로 묶어 같은 IP 의 동시 요청이 서버를 달리해도 함께 한도를 넘지 못함
// 키에 창 길이만큼 만료를 걸어 들른 IP 가 쌓이지 않음
@Component
@Slf4j
public class VisitorIssueLimiter {

    private static final int MAX_PER_WINDOW = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    // KEYS[1] IP 의 발급 기록(점수 = 발급 시각 ms)
    // ARGV 지금, 창 시작, 한도, 이번 기록 이름, 만료(ms)
    private static final RedisScript<Long> ISSUE = RedisScript.of(
            """
            redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', ARGV[2])
            if redis.call('ZCARD', KEYS[1]) >= tonumber(ARGV[3]) then return 0 end
            redis.call('ZADD', KEYS[1], ARGV[1], ARGV[4])
            redis.call('PEXPIRE', KEYS[1], ARGV[5])
            return 1
            """,
            Long.class);

    private final StringRedisTemplate redis;
    private final Clock clock;

    @Autowired
    public VisitorIssueLimiter(StringRedisTemplate redis) {
        this(redis, Clock.systemUTC());
    }

    // 테스트에서 시간을 옮겨 창 경계를 확인하는 용도
    VisitorIssueLimiter(StringRedisTemplate redis, Clock clock) {
        this.redis = redis;
        this.clock = clock;
    }

    public void check(String clientIp) {
        long now = clock.millis();
        Long allowed = redis.execute(
                ISSUE,
                List.of("limit:visitor:" + clientIp),
                String.valueOf(now),
                String.valueOf(now - WINDOW.toMillis()), // 이 시각 이하는 창 밖
                String.valueOf(MAX_PER_WINDOW),
                now + ":" + UUID.randomUUID(), // 같은 ms 에 두 번 와도 따로 셈
                String.valueOf(WINDOW.toMillis()));

        if (!Long.valueOf(1).equals(allowed)) {
            log.warn("비로그인 계정 발급 한도 초과 ip={}", clientIp);
            throw new CustomException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }
}
