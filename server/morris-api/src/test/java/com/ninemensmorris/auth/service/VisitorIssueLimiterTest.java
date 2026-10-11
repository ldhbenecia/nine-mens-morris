package com.ninemensmorris.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ninemensmorris.common.exception.CustomException;
import com.ninemensmorris.common.response.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VisitorIssueLimiterTest {

    private static final String IP = "10.0.0.1";

    private final MovableClock clock = new MovableClock(Instant.parse("2026-10-11T00:00:00Z"));
    private final VisitorIssueLimiter limiter = new VisitorIssueLimiter(clock);

    private void 발급(int count) {
        for (int i = 0; i < count; i++) {
            limiter.check(IP);
        }
    }

    @Test
    @DisplayName("한 IP 가 한도를 넘으면 거절한다")
    void 한도를_넘으면_거절한다() {
        // given
        발급(10);

        // when / then
        assertThatThrownBy(() -> limiter.check(IP))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("한도는 IP 마다 따로 센다")
    void 한도는_IP_마다_따로_센다() {
        // given
        발급(10);

        // when / then
        assertThatCode(() -> limiter.check("10.0.0.2")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("창의 경계를 넘어도 최근 1시간 안의 발급이 한도면 거절한다")
    void 경계_직후에도_막는다() {
        // given — 1시간 창의 끝자락에 한도를 채운다
        clock.move(Duration.ofMinutes(59).plusSeconds(59));
        발급(10);

        // when — 정각을 2초 넘김. 고정 창이었다면 여기서 새로 10건이 통과
        clock.move(Duration.ofSeconds(2));

        // then
        assertThatThrownBy(() -> limiter.check(IP)).isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("발급한 지 1시간이 지나면 다시 허용한다")
    void 한시간이_지나면_풀린다() {
        // given
        발급(10);

        // when
        clock.move(Duration.ofHours(1).plusSeconds(1));

        // then
        assertThatCode(() -> limiter.check(IP)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("정리 배치는 1시간이 지난 IP 만 지운다")
    void 오래된_IP_만_정리한다() {
        // given
        limiter.check("10.0.0.1");
        clock.move(Duration.ofMinutes(50));
        limiter.check("10.0.0.2");

        // when — 첫 IP 만 1시간을 넘김
        clock.move(Duration.ofMinutes(11));
        limiter.purgeExpired();

        // then
        assertThat(limiter.trackedIps()).isEqualTo(1);
    }

    private static final class MovableClock extends Clock {

        private Instant now;

        private MovableClock(Instant now) {
            this.now = now;
        }

        private void move(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
