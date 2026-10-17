package com.ninemensmorris.game.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ninemensmorris.core.board.Stone;
import com.ninemensmorris.core.move.Move;
import com.ninemensmorris.game.repository.RoomStore;
import com.ninemensmorris.support.IntegrationTestSupport;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

// 다른 서버는 같은 Redis 를 보는 저장소 인스턴스 하나 더로 흉내 냄
class RedisRoomRegistryTest extends IntegrationTestSupport implements RoomRegistryContract {

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private ObjectMapper objectMapper;

    private final List<Object> published = new ArrayList<>();
    private RoomRegistry registry;
    private RoomRegistry otherServer;

    @BeforeEach
    void setUp() {
        // 다른 통합 테스트가 만든 방이 목록·개수 검증에 섞이지 않게
        redis.execute((RedisCallback<Object>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
        published.clear();
        registry = new RedisRoomRegistry(new RoomStore(redis, objectMapper), published::add);
        otherServer = new RedisRoomRegistry(new RoomStore(redis, objectMapper), published::add);
    }

    @Override
    public RoomRegistry registry() {
        return registry;
    }

    @Override
    public RoomRegistry otherServer() {
        return otherServer;
    }

    @Test
    @DisplayName("로비에 보이는 값이 바뀔 때마다 로비 알림이 나간다")
    void 로비에_보이는_변화는_알린다() {
        // when — 생성·입장·시작·종료·삭제
        long roomId = registry.create("방", HOST).roomId();
        registry.mutate(roomId, room -> {
            room.join(GUEST);
            return null;
        });
        registry.mutate(roomId, room -> room.start());
        registry.mutate(roomId, room -> room.game().apply(room.stoneOf(HOST), new Move.Resign()));
        registry.mutate(roomId, room -> {
            room.close();
            return null;
        });

        // then
        assertThat(published)
                .containsExactly(
                        new LobbyChanged(roomId),
                        new LobbyChanged(roomId),
                        new LobbyChanged(roomId),
                        new LobbyChanged(roomId),
                        new LobbyChanged(roomId));
    }

    @Test
    @DisplayName("판 안에서만 보이는 변화나 헛된 요청은 로비에 알리지 않는다")
    void 판_안의_변화는_알리지_않는다() {
        // given
        long roomId = registry.create("방", HOST).roomId();
        registry.mutate(roomId, room -> {
            room.join(GUEST);
            room.changeFirstMoveRule(FirstMoveRule.HOST_FIRST);
            room.start();
            return null;
        });
        published.clear();

        // when — 착수 하나, 차례가 아닌 수 하나
        registry.mutate(roomId, room -> room.game().apply(Stone.BLACK, new Move.Place(0)));
        registry.mutate(roomId, room -> room.game().apply(Stone.BLACK, new Move.Place(1)));

        // then — 수마다 로비 전원이 목록을 다시 받으면 로비 트래픽이 진행 중인 판 수에 비례함
        assertThat(published).isEmpty();
    }
}
