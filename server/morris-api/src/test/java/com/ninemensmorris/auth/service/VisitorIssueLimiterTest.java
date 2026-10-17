package com.ninemensmorris.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ninemensmorris.common.exception.CustomException;
import com.ninemensmorris.common.response.ErrorCode;
import com.ninemensmorris.support.IntegrationTestSupport;
import com.ninemensmorris.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

// 카운터가 Redis 에 있어 실제 Redis 로 돌림. 시간은 시계를 옮겨 창 경계를 확인
class VisitorIssueLimiterTest extends IntegrationTestSupport {

    private static final String IP = "10.0.0.1";

    @Autowired
    private StringRedisTemplate redis;

    private MutableClock clock;
    private VisitorIssueLimiter limiter;

    @BeforeEach
    void setUp() {
        redis.delete(redis.keys("limit:visitor:*"));
        clock = new MutableClock(Instant.parse("2026-10-11T00:00:00Z"));
        limiter = new VisitorIssueLimiter(redis, clock);
    }

    private void 발급(int count) {
        for (int i = 0; i < count; i++) {
            limiter.check(IP);
        }
    }

    @Test
    @DisplayName("한 IP 가 짧은 시간에 한도를 넘으면 거절한다")
    void 한도를_넘으면_거절한다() {
        // given
        발급(5);

        // when / then
        assertThatThrownBy(() -> limiter.check(IP))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("한도는 IP 마다 따로 센다")
    void 한도는_IP_마다_따로_센다() {
        // given
        발급(5);

        // when / then
        assertThatCode(() -> limiter.check("10.0.0.2")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("서버가 여러 대여도 같은 IP 의 발급을 합쳐서 센다")
    void 서버가_달라도_합쳐서_센다() {
        // given — 서버 A 에서 3번, 서버 B 에서 2번. 메모리 카운터였다면 각자 5번까지 통과
        VisitorIssueLimiter otherServer = new VisitorIssueLimiter(redis, clock);
        발급(3);
        otherServer.check(IP);
        otherServer.check(IP);

        // when / then
        assertThatThrownBy(() -> otherServer.check(IP)).isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("몰아치지 않으면 발급 횟수에 상한이 없다")
    void 천천히_발급하면_막지_않는다() {
        // when / then — 15초 간격이면 1분 창 안에 4건뿐
        for (int i = 0; i < 20; i++) {
            assertThatCode(() -> limiter.check(IP)).doesNotThrowAnyException();
            clock.move(Duration.ofSeconds(15));
        }
    }

    @Test
    @DisplayName("창의 경계를 넘어도 최근 1분 안의 발급이 한도면 거절한다")
    void 경계_직후에도_막는다() {
        // given — 1분 창의 끝자락에 한도를 채움
        clock.move(Duration.ofSeconds(59));
        발급(5);

        // when — 분이 바뀌고 2초. 고정 창이었다면 여기서 새로 5건이 통과
        clock.move(Duration.ofSeconds(2));

        // then
        assertThatThrownBy(() -> limiter.check(IP)).isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("발급한 지 1분이 지나면 다시 허용한다")
    void 일분이_지나면_풀린다() {
        // given
        발급(5);

        // when
        clock.move(Duration.ofMinutes(1).plusSeconds(1));

        // then
        assertThatCode(() -> limiter.check(IP)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("기록에는 만료가 걸려 들른 IP 가 쌓이지 않는다")
    void 기록은_스스로_사라진다() {
        // when
        limiter.check(IP);

        // then — 만료가 없으면 들른 IP 를 지우는 정리 배치가 따로 필요함
        Long ttl = redis.getExpire("limit:visitor:" + IP);
        assertThat(ttl).isPositive().isLessThanOrEqualTo(60);
    }
}
