package com.ninemensmorris.game.domain;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

// 사용자별 활성 소켓 세션과 각 세션이 보고 있는 방
//
// 소켓이 끊겼다고 곧바로 방을 떠난 것으로 볼 수 없음
// 화면을 이동하면 이전 페이지의 소켓이 끊기고 새 페이지에서 다시 연결되며
// 새로고침이나 잠깐의 네트워크 끊김도 마찬가지임
//
// 다만 "같은 사용자의 세션이 하나라도 있으면 접속 중" 으로 보면 안 됨
// 로비를 다른 탭에 띄워 둔 채로 게임 탭만 닫으면 정산이 영구히 안 돌았다
// 그래서 방까지 같이 보고 "그 방에 붙어 있는 세션" 이 있는지로 판단함
@Component
public class SessionTracker {

    private final Map<Long, Set<String>> sessionsByUser = new ConcurrentHashMap<>();
    private final Map<String, Long> userBySession = new ConcurrentHashMap<>();
    private final Map<String, Long> roomBySession = new ConcurrentHashMap<>();

    // 세션 집합 변경을 compute 안에서 끝냄
    // 밖에서 add 하면 접속과 해제가 다른 스레드에서 겹칠 때 살아 있는 세션이 맵에서 빠진다
    public void add(long userId, String sessionId) {
        userBySession.put(sessionId, userId);
        sessionsByUser.compute(userId, (ignored, sessions) -> {
            Set<String> updated = sessions == null ? ConcurrentHashMap.newKeySet() : sessions;
            updated.add(sessionId);
            return updated;
        });
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
        sessionsByUser.compute(userId, (ignored, sessions) -> {
            if (sessions == null) {
                return null;
            }
            sessions.remove(sessionId);
            return sessions.isEmpty() ? null : sessions;
        });
        return Optional.of(new Departure(userId, roomId));
    }

    // 열려 있는 소켓 수. 사용자 수가 아니라 세션 수다
    // 한 사람이 로비와 게임을 두 탭에 띄우면 2로 센다
    public int openSessions() {
        return userBySession.size();
    }

    // 이 사용자가 해당 방에 아직 붙어 있는지
    public boolean isWatching(long userId, long roomId) {
        Set<String> sessions = sessionsByUser.get(userId);
        if (sessions == null) {
            return false;
        }
        return sessions.stream().anyMatch(sessionId -> Long.valueOf(roomId).equals(roomBySession.get(sessionId)));
    }

    // 끊긴 세션의 주인과 그 세션이 보던 방. 방을 안 보고 있었으면 roomId 는 null
    public record Departure(long userId, Long roomId) {}
}
