package com.ninemensmorris.game.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ninemensmorris.game.repository.RoomStore;
import com.ninemensmorris.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

// 다른 서버는 같은 Redis 를 보는 저장소 인스턴스 하나 더로 흉내 냄
class RedisRoomRegistryTest extends IntegrationTestSupport implements RoomRegistryContract {

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private ObjectMapper objectMapper;

    private RoomRegistry registry;
    private RoomRegistry otherServer;

    @BeforeEach
    void setUp() {
        // 다른 통합 테스트가 만든 방이 목록·개수 검증에 섞이지 않게
        redis.execute((RedisCallback<Object>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
        registry = new RedisRoomRegistry(new RoomStore(redis, objectMapper));
        otherServer = new RedisRoomRegistry(new RoomStore(redis, objectMapper));
    }

    @Override
    public RoomRegistry registry() {
        return registry;
    }

    @Override
    public RoomRegistry otherServer() {
        return otherServer;
    }
}
