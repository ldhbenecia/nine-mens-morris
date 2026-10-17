package com.ninemensmorris.config;

import com.ninemensmorris.game.domain.SessionTracker;
import com.ninemensmorris.game.service.GameService;
import com.ninemensmorris.messaging.ClusterBroadcaster;
import com.ninemensmorris.messaging.ClusterPresence;
import com.ninemensmorris.messaging.StompDestinations;
import com.ninemensmorris.security.AuthenticatedUser;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.RejectedExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
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

    // 이 시간 안에 다시 접속하면 나간 것으로 보지 않음
    // 없으면 새로고침·화면 이동만으로 나간 것으로 처리돼 방이 사라짐
    private static final Duration RECONNECT_GRACE = Duration.ofSeconds(10);

    private final GameService gameService;
    private final SessionTracker sessions; // 이 서버의 세션. 끊긴 세션이 어느 방을 보던 것인지
    private final ClusterPresence presence; // 서버 전체 기준 "그 방에 붙어 있는가"
    private final ClusterBroadcaster broadcaster;
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
    // 구독 검사(ClientFrameGuard)가 버린 구독은 이 이벤트가 오지 않음
    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        AuthenticatedUser actor = AuthenticatedUser.from(accessor.getUser());
        String sessionId = accessor.getSessionId();
        StompDestinations.roomIdFromTopic(accessor.getDestination()).ifPresent(roomId -> {
            // 같은 세션이 같은 방을 다시 구독해도 한 번만 셈
            if (actor == null || !sessions.enterRoom(sessionId, roomId)) {
                return;
            }
            broadcaster.watch(roomId);
            presence.enter(actor.id(), roomId, sessionId);
        });
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        SessionTracker.Departure departure = sessions.remove(sessionId).orElse(null);
        if (departure == null) {
            return;
        }
        for (long roomId : departure.rooms()) {
            log.debug("소켓 해제 userId={} roomId={} sessionId={}", departure.userId(), roomId, sessionId);
            broadcaster.unwatch(roomId);
            presence.leave(departure.userId(), roomId, sessionId);
            scheduleSettlement(departure.userId(), roomId);
        }
    }

    private void scheduleSettlement(long userId, long roomId) {
        try {
            taskScheduler.schedule(
                    () -> settleIfStillGone(userId, roomId), Instant.now().plus(RECONNECT_GRACE));
        } catch (RejectedExecutionException rejected) {
            // 서버가 내려가는 중. 이때 붙어 있던 사람 수만큼 예외가 터져 로그를 덮는다
            // 정산을 건너뛰어도 됨. 클라이언트는 다른 서버로 다시 붙고, 안 돌아오면 턴 제한 시간이 판을 끝냄
            log.debug("종료 중이라 재접속 유예를 예약하지 못함 userId={} roomId={}", userId, roomId);
        }
    }

    // 다른 서버로 다시 붙었을 수 있어 서버 전체 기준으로 봄
    private void settleIfStillGone(long userId, long roomId) {
        if (presence.isWatching(userId, roomId)) {
            return;
        }

        gameService
                .handleDisconnect(userId, roomId)
                .ifPresent(broadcast -> broadcaster.sendToRoom(broadcast.roomId(), broadcast.event()));
    }
}
