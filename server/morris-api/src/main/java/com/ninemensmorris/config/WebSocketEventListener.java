package com.ninemensmorris.config;

import com.ninemensmorris.game.domain.SessionTracker;
import com.ninemensmorris.game.service.GameService;
import com.ninemensmorris.security.AuthenticatedUser;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventListener {

    private static final String ROOM_TOPIC_PREFIX = "/topic/rooms/";

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

    // 방 토픽 구독이 "이 세션이 그 방을 보고 있다" 는 유일한 신호
    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        roomIdOf(accessor.getDestination()).ifPresent(roomId -> sessions.enterRoom(accessor.getSessionId(), roomId));
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        SessionTracker.Departure departure = sessions.remove(sessionId).orElse(null);
        if (departure == null || departure.roomId() == null) {
            return;
        }
        log.debug("소켓 해제 userId={} roomId={} sessionId={}", departure.userId(), departure.roomId(), sessionId);

        long userId = departure.userId();
        long roomId = departure.roomId();
        taskScheduler.schedule(
                () -> settleIfStillGone(userId, roomId), Instant.now().plus(RECONNECT_GRACE));
    }

    private void settleIfStillGone(long userId, long roomId) {
        if (sessions.isWatching(userId, roomId)) {
            return;
        }

        gameService
                .handleDisconnect(userId, roomId)
                .ifPresent(broadcast ->
                        messaging.convertAndSend(ROOM_TOPIC_PREFIX + broadcast.roomId(), broadcast.event()));
    }

    private Optional<Long> roomIdOf(String destination) {
        if (destination == null || !destination.startsWith(ROOM_TOPIC_PREFIX)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(destination.substring(ROOM_TOPIC_PREFIX.length())));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }
}
