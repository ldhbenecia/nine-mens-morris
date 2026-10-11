package com.ninemensmorris.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.ninemensmorris.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;

// 하트비트는 설정을 빼먹어도 아무 오류가 없음. 협상이 0 으로 끝나 조용히 꺼짐
class WebSocketHeartbeatTest extends IntegrationTestSupport {

    @Autowired
    private SimpleBrokerMessageHandler brokerHandler;

    @Test
    @DisplayName("브로커가 하트비트를 주고받는다")
    void 하트비트가_켜져_있다() {
        // then — 0 이면 끊긴 소켓을 TCP 타임아웃까지 모름
        assertThat(brokerHandler.getHeartbeatValue())
                .containsExactly(WebSocketConfig.HEARTBEAT_MILLIS, WebSocketConfig.HEARTBEAT_MILLIS);
        assertThat(brokerHandler.getTaskScheduler()).isNotNull();
    }
}
