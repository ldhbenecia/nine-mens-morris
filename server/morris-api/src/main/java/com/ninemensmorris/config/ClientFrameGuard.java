package com.ninemensmorris.config;

import com.ninemensmorris.game.domain.RoomRegistry;
import com.ninemensmorris.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

// 클라이언트가 보낸 STOMP 프레임 검사
//
// 전송(SEND)뿐 아니라 연결(CONNECT)과 구독(SUBSCRIBE)도 막아야 함
// 예전에는 SEND 만 봐서, 쿠키 없이 연결한 뒤 /topic/rooms/* 를 구독하면
// 진행 중인 모든 게임의 판 전체와 양쪽 userId 를 실시간으로 받아볼 수 있었다
@Component
@RequiredArgsConstructor
@Slf4j
public class ClientFrameGuard implements ChannelInterceptor {

    private static final String APP_PREFIX = "/app/";
    private static final String ROOM_TOPIC_PREFIX = "/topic/rooms/";
    private static final String LOBBY_TOPIC = "/topic/lobby";
    private static final String USER_PREFIX = "/user/";

    private final RoomRegistry rooms;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        SimpMessageType type = accessor.getMessageType();
        if (type == null) {
            return message;
        }

        return switch (type) {
            case CONNECT -> requireAuthenticated(message, accessor);
            case SUBSCRIBE -> authorizeSubscribe(message, accessor);
            case MESSAGE -> requireAppDestination(message, accessor);
            default -> message;
        };
    }

    // 인증 없는 소켓은 붙는 것 자체를 막음
    // 익명 연결을 허용하면 아무나 무한히 소켓을 열어 둘 수 있다
    private Message<?> requireAuthenticated(Message<?> message, StompHeaderAccessor accessor) {
        if (AuthenticatedUser.from(accessor.getUser()) == null) {
            throw new IllegalArgumentException("인증되지 않은 소켓 연결");
        }
        return message;
    }

    // 구독 거부는 예외를 던지지 않고 프레임만 버림
    // 예외를 던지면 연결이 끊기고, 끊기면 클라이언트가 다시 붙어 또 거부당하는 루프가 된다
    // 받을 자격이 없는 사람에게 아무것도 가지 않는다는 목적은 프레임을 버리는 것으로 충분함
    private Message<?> authorizeSubscribe(Message<?> message, StompHeaderAccessor accessor) {
        AuthenticatedUser actor = AuthenticatedUser.from(accessor.getUser());
        if (actor == null) {
            return null;
        }

        String destination = accessor.getDestination();
        if (destination == null) {
            return null;
        }
        // 사용자 목적지는 Spring 이 principal 별로 갈라주므로 남의 큐를 받을 수 없음
        if (destination.startsWith(USER_PREFIX) || destination.equals(LOBBY_TOPIC)) {
            return message;
        }
        if (destination.startsWith(ROOM_TOPIC_PREFIX)) {
            return isMember(destination, actor.id()) ? message : denied(actor.id(), destination);
        }
        return denied(actor.id(), destination);
    }

    private Message<?> denied(long userId, String destination) {
        log.debug("구독 거부 userId={} destination={}", userId, destination);
        return null;
    }

    private boolean isMember(String destination, long userId) {
        try {
            long roomId = Long.parseLong(destination.substring(ROOM_TOPIC_PREFIX.length()));
            return rooms.find(roomId).filter(room -> room.contains(userId)).isPresent();
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    // SimpleBroker 는 클라이언트가 /topic 으로 보낸 SEND 프레임도 구독자에게 그대로 중계한다
    // /app 접두사는 "여기로 보내면 @MessageMapping 이 받는다" 는 뜻일 뿐
    // 다른 곳으로 못 보낸다는 뜻이 아니다
    // 막지 않으면 브라우저 콘솔 한 줄로 상대 화면에 위조된 게임 상태를 띄울 수 있다
    private Message<?> requireAppDestination(Message<?> message, StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(APP_PREFIX)) {
            throw new IllegalArgumentException("클라이언트는 /app 으로만 전송할 수 있음: " + destination);
        }
        return message;
    }
}
