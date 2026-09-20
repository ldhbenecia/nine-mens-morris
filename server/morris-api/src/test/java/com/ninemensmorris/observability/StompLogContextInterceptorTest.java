package com.ninemensmorris.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.ninemensmorris.common.logging.LogContext;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class StompLogContextInterceptorTest {

    private final StompLogContextInterceptor interceptor = new StompLogContextInterceptor();

    @AfterEach
    void tearDown() {
        LogContext.clear();
    }

    private Message<byte[]> frame(String destination, Long userId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination(destination);
        if (userId != null) {
            accessor.setUser(new UsernamePasswordAuthenticationToken(userId, null, List.of()));
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    @DisplayName("목적지와 주체에서 traceId / userId / roomId 를 채운다")
    void 컨텍스트를_채운다() {
        // when
        interceptor.beforeHandle(frame("/app/rooms/42/place", 7L), null, null);

        // then
        assertThat(MDC.get(LogContext.TRACE_ID)).isNotBlank();
        assertThat(MDC.get(LogContext.USER_ID)).isEqualTo("7");
        assertThat(MDC.get(LogContext.ROOM_ID)).isEqualTo("42");
    }

    @Test
    @DisplayName("방과 무관한 목적지면 roomId 없이 traceId 만 붙는다")
    void 방이_없으면_roomId_는_비운다() {
        // when
        interceptor.beforeHandle(frame("/app/whatever", 7L), null, null);

        // then
        assertThat(MDC.get(LogContext.TRACE_ID)).isNotBlank();
        assertThat(MDC.get(LogContext.ROOM_ID)).isNull();
    }

    @Test
    @DisplayName("처리가 끝나면 지운다")
    void 처리_후_지운다() {
        // given — 스레드가 재사용되므로 남으면 다음 메시지에 남의 값이 붙는다
        Message<byte[]> message = frame("/app/rooms/42/place", 7L);
        interceptor.beforeHandle(message, null, null);

        // when
        interceptor.afterMessageHandled(message, null, null, null);

        // then
        assertThat(MDC.get(LogContext.TRACE_ID)).isNull();
        assertThat(MDC.get(LogContext.USER_ID)).isNull();
        assertThat(MDC.get(LogContext.ROOM_ID)).isNull();
    }
}
