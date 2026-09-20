package com.ninemensmorris.auth.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ninemensmorris.common.exception.CustomException;
import com.ninemensmorris.common.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// 인증 없이 부를 수 있는 쓰기 엔드포인트라 한도가 실제로 도는지 확인해야 한다
class VisitorIssueLimiterTest {

    private final VisitorIssueLimiter limiter = new VisitorIssueLimiter();

    @Test
    @DisplayName("한 IP 가 한도를 넘으면 거절한다")
    void 한도를_넘으면_거절한다() {
        // given — 한도까지는 통과한다
        for (int i = 0; i < 10; i++) {
            limiter.check("10.0.0.1");
        }

        // when / then
        assertThatThrownBy(() -> limiter.check("10.0.0.1"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("한도는 IP 마다 따로 센다")
    void 한도는_IP_마다_따로_센다() {
        // given — 한 사람이 한도를 채워도 다른 사람은 막히면 안 된다
        for (int i = 0; i < 10; i++) {
            limiter.check("10.0.0.1");
        }

        // when / then
        assertThatCode(() -> limiter.check("10.0.0.2")).doesNotThrowAnyException();
    }
}
