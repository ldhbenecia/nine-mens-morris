package com.ninemensmorris.messaging;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 사용자가 그 방에 아직 붙어 있는지. 서버 전체 기준
//
// 소켓이 끊겨도 다른 서버로 다시 붙었을 수 있어 이 서버의 세션만 보고는 판단할 수 없음
// 방·사용자별로 붙어 있는 세션을 "서버id:세션id" 로 모아 둠
//
// 죽은 서버의 세션은 해제 이벤트가 오지 않아 집합에 남음
// 서버마다 생존 키를 짧은 TTL 로 갱신하고, 생존 키가 없는 서버의 세션은 없는 것으로 봄
@Component
@Slf4j
public class ClusterPresence {

    private static final Duration ALIVE_TTL = Duration.ofSeconds(15);

    private final StringRedisTemplate redis;
    private final String instanceId;

    // 쿠버네티스에서는 HOSTNAME 이 파드 이름이라 로그·Redis 에서 어느 파드인지 바로 보임
    // 테스트는 이름을 직접 넘겨 서버 여러 대를 흉내 냄
    public ClusterPresence(StringRedisTemplate redis, @Value("${HOSTNAME:}") String hostname) {
        this.redis = redis;
        this.instanceId = hostname.isBlank() ? UUID.randomUUID().toString() : hostname;
        heartbeat();
    }

    public void enter(long userId, long roomId, String sessionId) {
        redis.opsForSet().add(key(roomId, userId), member(sessionId));
    }

    public void leave(long userId, long roomId, String sessionId) {
        redis.opsForSet().remove(key(roomId, userId), member(sessionId));
    }

    public boolean isWatching(long userId, long roomId) {
        Set<String> members = redis.opsForSet().members(key(roomId, userId));
        if (members == null) {
            return false;
        }
        return members.stream()
                .map(member -> member.substring(0, member.indexOf(':'))) // 서버 이름에는 ':' 가 없음
                .distinct()
                .anyMatch(server -> Boolean.TRUE.equals(redis.hasKey(aliveKey(server))));
    }

    // TTL 의 1/3 마다. 한두 번 놓쳐도 살아 있는 서버가 죽은 것으로 보이지 않게
    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.SECONDS)
    public void heartbeat() {
        redis.opsForValue().set(aliveKey(instanceId), "1", ALIVE_TTL);
    }

    // 정상 종료면 TTL 을 기다리지 않고 바로 빠짐
    @PreDestroy
    public void retire() {
        redis.delete(aliveKey(instanceId));
    }

    public String instanceId() {
        return instanceId;
    }

    private String member(String sessionId) {
        return instanceId + ":" + sessionId;
    }

    private static String key(long roomId, long userId) {
        return "presence:" + roomId + ":" + userId;
    }

    private static String aliveKey(String server) {
        return "instance:" + server;
    }
}
