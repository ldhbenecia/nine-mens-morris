package com.ninemensmorris.config;

import com.ninemensmorris.game.domain.SessionTracker;
import com.ninemensmorris.game.service.GameService;
import com.ninemensmorris.security.AuthenticatedUser;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventListener {

    // 이 시간 안에 다시 접속하면 나간 것으로 보지 않음
    // 화면 이동만으로 방이 사라지던 문제를 막기 위함
    private static final Duration RECONNECT_GRACE = Duration.ofSeconds(10);

    private final GameService gameService;
    private final SessionTracker sessions;
    private final SimpMessagingTemplate messaging;
    private final TaskScheduler taskScheduler;

    @EventListener
    public void onConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        AuthenticatedUser actor = AuthenticatedUser.from(accessor.getUser());
        if (actor == null) {
            return;
        }

        sessions.add(actor.id(), accessor.getSessionId());
        log.debug("소켓 연결 userId={} sessionId={}", actor.id(), accessor.getSessionId());
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        Long userId = sessions.remove(sessionId).orElse(null);
        if (userId == null) {
            return;
        }
        log.debug("소켓 해제 userId={} sessionId={}", userId, sessionId);

        // 다른 탭이나 새 화면의 세션이 이미 있으면 나간 게 아님
        if (sessions.hasActiveSession(userId)) {
            return;
        }

        // 유예 시간 뒤에도 돌아오지 않았을 때만 정리
        taskScheduler.schedule(() -> settleIfStillGone(userId), Instant.now().plus(RECONNECT_GRACE));
    }

    private void settleIfStillGone(long userId) {
        if (sessions.hasActiveSession(userId)) {
            return;
        }

        gameService
                .handleDisconnect(userId)
                .ifPresent(
                        broadcast -> messaging.convertAndSend("/topic/rooms/" + broadcast.roomId(), broadcast.event()));
    }
}
