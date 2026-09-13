package com.ninemensmorris.game.domain;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

// 사용자별 활성 소켓 세션
//
// 소켓이 끊겼다고 곧바로 방을 떠난 것으로 볼 수 없음
// 화면을 이동하면 이전 페이지의 소켓이 끊기고 새 페이지에서 다시 연결되며
// 새로고침이나 잠깐의 네트워크 끊김도 마찬가지임
// 같은 사용자의 다른 세션이 남아 있으면 아직 접속 중인 것으로 봄
@Component
public class SessionTracker {

    private final Map<Long, Set<String>> sessionsByUser = new ConcurrentHashMap<>();
    private final Map<String, Long> userBySession = new ConcurrentHashMap<>();

    public void add(long userId, String sessionId) {
        userBySession.put(sessionId, userId);
        sessionsByUser
                .computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet())
                .add(sessionId);
    }

    // 끊긴 세션의 주인. 없으면 추적 대상이 아니던 세션임
    public Optional<Long> remove(String sessionId) {
        Long userId = userBySession.remove(sessionId);
        if (userId == null) {
            return Optional.empty();
        }
        sessionsByUser.computeIfPresent(userId, (ignored, sessions) -> {
            sessions.remove(sessionId);
            return sessions.isEmpty() ? null : sessions;
        });
        return Optional.of(userId);
    }

    public boolean hasActiveSession(long userId) {
        return sessionsByUser.containsKey(userId);
    }
}
