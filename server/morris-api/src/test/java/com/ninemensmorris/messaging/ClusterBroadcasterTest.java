package com.ninemensmorris.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ninemensmorris.support.IntegrationTestSupport;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

// 방 이벤트가 그 방을 보는 세션이 있는 서버로만 가는지
// 서버 둘은 같은 Redis 를 보는 브로드캐스터 둘로 흉내 내고, 각 서버의 STOMP 브로커는 목으로 둠
class ClusterBroadcasterTest extends IntegrationTestSupport {

    private static final String ROOM_7 = "/topic/rooms/7";
    private static final Map<String, Integer> PAYLOAD = Map.of("move", 1);

    // 채널 구독은 비동기라 막 구독한 직후의 발행을 놓칠 수 있음. 운영에서는 sync 가 메움
    private static final long SUBSCRIBE_SETTLE_MILLIS = 300;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisConnectionFactory connectionFactory;

    private SimpMessagingTemplate brokerA;
    private SimpMessagingTemplate brokerB;
    private ClusterBroadcaster serverA;
    private ClusterBroadcaster serverB;

    @BeforeEach
    void setUp() throws InterruptedException {
        brokerA = mock(SimpMessagingTemplate.class);
        brokerB = mock(SimpMessagingTemplate.class);
        serverA = new ClusterBroadcaster(redis, brokerA, objectMapper, connectionFactory);
        serverB = new ClusterBroadcaster(redis, brokerB, objectMapper, connectionFactory);
        serverA.start();
        serverB.start();
        Thread.sleep(SUBSCRIBE_SETTLE_MILLIS);
    }

    @AfterEach
    void tearDown() throws Exception {
        serverA.stop();
        serverB.stop();
    }

    @Test
    @DisplayName("그 방을 보는 세션이 없는 서버에는 방 이벤트가 가지 않는다")
    void 구독자_없는_서버는_받지_않는다() throws InterruptedException {
        // given — 7번 방의 두 사람이 모두 서버 A 에 붙어 있음
        serverA.watch(7);
        Thread.sleep(SUBSCRIBE_SETTLE_MILLIS);

        // when — 어느 서버에서 보내든
        serverB.send(ROOM_7, PAYLOAD);

        // then — 채널 하나로 뿌리던 때는 서버 B 도 받아서 버렸음
        verify(brokerA, timeout(2000)).convertAndSend(eq(ROOM_7), any(Object.class));
        verify(brokerB, after(500).never()).convertAndSend(eq(ROOM_7), any(Object.class));
    }

    @Test
    @DisplayName("두 사람이 다른 서버에 붙어 있으면 두 서버가 모두 받는다")
    void 두_서버가_모두_받는다() throws InterruptedException {
        // given
        serverA.watch(7);
        serverB.watch(7);
        Thread.sleep(SUBSCRIBE_SETTLE_MILLIS);

        // when
        serverA.send(ROOM_7, PAYLOAD);

        // then
        verify(brokerA, timeout(2000)).convertAndSend(eq(ROOM_7), any(Object.class));
        verify(brokerB, timeout(2000)).convertAndSend(eq(ROOM_7), any(Object.class));
    }

    @Test
    @DisplayName("그 방을 보던 마지막 세션이 떠나면 그 서버는 더 받지 않는다")
    void 마지막_세션이_떠나면_구독을_푼다() throws InterruptedException {
        // given — 같은 서버에 두 세션
        serverA.watch(7);
        serverA.watch(7);
        serverA.unwatch(7);
        Thread.sleep(SUBSCRIBE_SETTLE_MILLIS);
        serverA.send(ROOM_7, PAYLOAD);
        verify(brokerA, timeout(2000)).convertAndSend(eq(ROOM_7), any(Object.class));

        // when — 남은 한 세션도 떠남
        serverA.unwatch(7);
        Thread.sleep(SUBSCRIBE_SETTLE_MILLIS);
        serverB.send(ROOM_7, PAYLOAD);

        // then — 처음 한 번 받은 것 말고는 없음
        verify(brokerA, after(500).times(1)).convertAndSend(eq(ROOM_7), any(Object.class));
    }

    @Test
    @DisplayName("로비 신호는 모든 서버가 받는다")
    void 로비는_모두_받는다() {
        // when
        serverA.send("/topic/lobby", PAYLOAD);

        // then
        verify(brokerA, timeout(2000)).convertAndSend(eq("/topic/lobby"), any(Object.class));
        verify(brokerB, timeout(2000)).convertAndSend(eq("/topic/lobby"), any(Object.class));
    }
}
