package com.ninemensmorris.game.domain;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

// 이 서버에 붙은 소켓 세션과 각 세션이 보고 있는 방
//
// 세션이 끊겼을 때 누구의 어느 방이었는지 알려고 둠. 해제 이벤트에는 세션 id 만 옴
// "그 사용자가 아직 방에 붙어 있는가" 는 다른 서버로 다시 붙었을 수 있어 여기서 판단하지 않음
// 그 판단은 ClusterPresence 가 서버 전체 기준으로 함
@Component
public class SessionTracker {

    private final Map<String, Long> userBySession = new ConcurrentHashMap<>();
    private final Map<String, Long> roomBySession = new ConcurrentHashMap<>();

    public void add(long userId, String sessionId) {
        userBySession.put(sessionId, userId);
    }

    // 이 세션이 어느 방을 보고 있는지. 방 토픽 구독 시점에 알 수 있음
    public void enterRoom(String sessionId, long roomId) {
        if (userBySession.containsKey(sessionId)) {
            roomBySession.put(sessionId, roomId);
        }
    }

    public Optional<Departure> remove(String sessionId) {
        Long userId = userBySession.remove(sessionId);
        Long roomId = roomBySession.remove(sessionId);
        if (userId == null) {
            return Optional.empty();
        }
        return Optional.of(new Departure(userId, roomId));
    }

    // 이 서버에 열려 있는 소켓 수. 사용자 수가 아니라 세션 수다
    // 한 사람이 로비와 게임을 두 탭에 띄우면 2로 센다
    public int openSessions() {
        return userBySession.size();
    }

    // 끊긴 세션의 주인과 그 세션이 보던 방. 방을 안 보고 있었으면 roomId 는 null
    public record Departure(long userId, Long roomId) {}
}
