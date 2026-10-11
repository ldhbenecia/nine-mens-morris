package com.ninemensmorris.auth.service;

import com.ninemensmorris.common.exception.CustomException;
import com.ninemensmorris.common.response.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 비로그인 계정 발급 한도. 인증 없이 부르는 쓰기 엔드포인트라 막지 않으면 users 행이 무한히 생김
//
// 짧은 시간에 몰아치는 발급만 막음. 그보다 느린 발급은 쌓여도 정리 배치가 놀고 있는 계정을 지움
// 시간 단위로 묶으면 새로고침·탭 여러 개처럼 평범한 사용도 막혀 버림
//
// 슬라이딩 창 — "지금부터 1분 전까지" 의 발급 수로 판단함
// 고정 창은 경계 앞뒤 몇 초 사이에 한도의 두 배가 통과함
//
// 카운터가 메모리에 있어 서버가 여러 대면 한도가 대수만큼 늘어남. 그때는 Redis 구현으로 교체
@Component
@Slf4j
public class VisitorIssueLimiter {

    private static final int MAX_PER_WINDOW = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final Clock clock;
    private final Map<String, Deque<Instant>> issuedAt = new ConcurrentHashMap<>(); // IP 별 최근 발급 시각, 최대 5개

    public VisitorIssueLimiter() {
        this(Clock.systemUTC());
    }

    // 테스트에서 시간을 옮겨 창 경계를 확인하는 용도
    VisitorIssueLimiter(Clock clock) {
        this.clock = clock;
    }

    public void check(String clientIp) {
        Instant now = clock.instant();
        Instant since = now.minus(WINDOW);
        boolean[] allowed = {false};

        // compute 는 키 단위로 원자적이라 같은 IP 의 동시 요청이 함께 한도를 넘지 못함
        issuedAt.compute(clientIp, (ip, times) -> {
            Deque<Instant> recent = times == null ? new ArrayDeque<>() : times;
            dropOlderThan(recent, since);
            if (recent.size() < MAX_PER_WINDOW) {
                recent.addLast(now);
                allowed[0] = true;
            }
            return recent;
        });

        if (!allowed[0]) {
            log.warn("비로그인 계정 발급 한도 초과 ip={}", clientIp);
            throw new CustomException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }

    // 지우지 않으면 한 번 들른 IP 가 전부 남아 메모리가 계속 늚
    // 키마다 computeIfPresent 로 지워야 그 사이 들어온 발급 기록을 함께 날리지 않음
    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    void purgeExpired() {
        Instant since = clock.instant().minus(WINDOW);
        for (String ip : issuedAt.keySet()) {
            issuedAt.computeIfPresent(ip, (key, times) -> {
                dropOlderThan(times, since);
                return times.isEmpty() ? null : times;
            });
        }
    }

    int trackedIps() {
        return issuedAt.size();
    }

    private static void dropOlderThan(Deque<Instant> times, Instant since) {
        while (!times.isEmpty() && !times.peekFirst().isAfter(since)) {
            times.pollFirst();
        }
    }
}
