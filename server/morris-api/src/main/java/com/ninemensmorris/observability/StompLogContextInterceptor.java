package com.ninemensmorris.observability;

import com.ninemensmorris.common.logging.LogContext;
import com.ninemensmorris.security.AuthenticatedUser;
import java.util.Optional;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

// STOMP 메시지에 traceId / userId / roomId 를 붙인다. 서블릿 필터가 닿지 않는 경로다
//
// ChannelInterceptor 가 아니라 ExecutorChannelInterceptor 여야 한다
// preSend 는 보낸 쪽 스레드에서 돌고 핸들러는 실행기 스레드에서 돌아 MDC 가 전달되지 않는다
@Component
public class StompLogContextInterceptor implements ExecutorChannelInterceptor {

    private static final String APP_ROOM_PREFIX = "/app/rooms/";

    @Override
    public Message<?> beforeHandle(Message<?> message, MessageChannel channel, MessageHandler handler) {
        LogContext.startTrace();

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        AuthenticatedUser actor = AuthenticatedUser.from(accessor.getUser());
        if (actor != null) {
            LogContext.putUserId(actor.id());
        }
        roomIdOf(accessor.getDestination()).ifPresent(LogContext::putRoomId);
        return message;
    }

    @Override
    public void afterMessageHandled(
            Message<?> message, MessageChannel channel, MessageHandler handler, Exception exception) {
        LogContext.clear();
    }

    // /app/rooms/{roomId}/place -> roomId
    private Optional<Long> roomIdOf(String destination) {
        if (destination == null || !destination.startsWith(APP_ROOM_PREFIX)) {
            return Optional.empty();
        }
        String rest = destination.substring(APP_ROOM_PREFIX.length());
        int slash = rest.indexOf('/');
        try {
            return Optional.of(Long.parseLong(slash < 0 ? rest : rest.substring(0, slash)));
        } catch (NumberFormatException notARoom) {
            return Optional.empty();
        }
    }
}
