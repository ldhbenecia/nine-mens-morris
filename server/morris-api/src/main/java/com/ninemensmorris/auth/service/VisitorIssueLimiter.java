package com.ninemensmorris.auth.service;

import com.ninemensmorris.common.exception.CustomException;
import com.ninemensmorris.common.response.ErrorCode;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 비로그인 계정 발급은 인증 없이 부를 수 있는 쓰기 엔드포인트다
// 막지 않으면 users 에 행이 무한히 생긴다
//
// 서버가 한 대라 카운터를 메모리에 둔다. 여러 대가 되면 이 구현으로는 한도가 대수만큼 늘어나므로
// 그때는 같은 인터페이스를 Redis 구현으로 바꿔야 한다
@Component
@Slf4j
public class VisitorIssueLimiter {

    private static final int MAX_PER_WINDOW = 10;
    private static final Duration WINDOW = Duration.ofHours(1);

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    public void check(String clientIp) {
        Instant now = Instant.now();
        // compute 는 키 단위로 원자적이라 같은 IP 의 동시 요청이 창을 각자 새로 열지 못함
        Counter counter = counters.compute(
                clientIp, (ip, current) -> current == null || current.isExpired(now) ? new Counter(now) : current);

        if (counter.increment() > MAX_PER_WINDOW) {
            log.warn("비로그인 계정 발급 한도 초과 ip={}", clientIp);
            throw new CustomException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }

    // 지우지 않으면 한 번 들른 IP 가 전부 남아 메모리가 계속 는다
    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    void purgeExpired() {
        Instant now = Instant.now();
        counters.values().removeIf(counter -> counter.isExpired(now));
    }

    private static final class Counter {

        private final Instant startedAt;
        private final AtomicInteger count = new AtomicInteger();

        private Counter(Instant startedAt) {
            this.startedAt = startedAt;
        }

        private boolean isExpired(Instant now) {
            return startedAt.plus(WINDOW).isBefore(now);
        }

        private int increment() {
            return count.incrementAndGet();
        }
    }
}
