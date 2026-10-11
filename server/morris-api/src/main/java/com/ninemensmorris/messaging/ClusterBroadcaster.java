package com.ninemensmorris.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

// 방·로비 토픽을 그 토픽을 구독한 세션이 있는 서버로만 보냄
//
// 서버마다 SimpleBroker 가 자기에게 붙은 세션만 알아서 한 서버에서 보내면 다른 서버에 붙은 상대는 못 받음
// 방마다 Redis 채널을 두고, 그 방 구독자가 이 서버에 생기면 채널을 구독하고 다 떠나면 해제함
// 채널 하나로 모든 서버에 뿌리면 서버가 8대일 때 두 사람의 판이 나머지 6대에도 가서 버려짐
//
// 보낸 서버도 채널을 거쳐 받으므로 자기 브로커로 직접 보내지 않음. 직접 보내면 두 번 감
// 로비는 방 생성·입장·퇴장 때만 오는 드문 신호라 모든 서버가 늘 구독함
//
// pub/sub 은 전달을 보장하지 않음. 받는 서버가 그 순간 죽어 있거나 막 구독하는 중이면 놓치지만
// 클라이언트가 구독 직후와 재접속 때 sync 로 판 전체를 받아 메워짐
//
// 개인 큐(/user/queue/*)는 여기로 보내지 않음. 그 프레임을 보낸 세션이 이 서버에 있음
@Component
@Slf4j
public class ClusterBroadcaster implements MessageListener {

    private static final String ROOM_TOPIC_PREFIX = "/topic/rooms/";
    private static final String LOBBY_TOPIC = "/topic/lobby";
    private static final String LOBBY_CHANNEL = "morris:lobby";

    private final StringRedisTemplate redis;
    private final SimpMessagingTemplate messaging;
    private final ObjectMapper objectMapper;
    private final RedisMessageListenerContainer container = new RedisMessageListenerContainer();

    // 이 서버에서 방마다 그 방을 보고 있는 세션 수. 0 이 되면 채널 구독을 해제함
    private final Map<Long, Integer> localWatchers = new ConcurrentHashMap<>();

    public ClusterBroadcaster(
            StringRedisTemplate redis,
            SimpMessagingTemplate messaging,
            ObjectMapper objectMapper,
            RedisConnectionFactory connectionFactory) {
        this.redis = redis;
        this.messaging = messaging;
        this.objectMapper = objectMapper;
        // 구독 전용 연결을 따로 잡음. 구독 중인 연결로는 다른 명령을 보낼 수 없음
        this.container.setConnectionFactory(connectionFactory);
    }

    @PostConstruct
    void start() {
        container.addMessageListener(this, new ChannelTopic(LOBBY_CHANNEL));
        container.afterPropertiesSet();
        container.start();
    }

    @PreDestroy
    void stop() throws Exception {
        container.destroy();
    }

    public void send(String destination, Object payload) {
        try {
            String envelope = objectMapper.writeValueAsString(new Envelope(destination, payload));
            redis.convertAndSend(channelOf(destination), envelope);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("토픽 메시지 직렬화 실패 destination=" + destination, exception);
        }
    }

    // 이 서버의 세션 하나가 그 방을 보기 시작함. 첫 세션이면 채널을 구독
    public void watch(long roomId) {
        localWatchers.compute(roomId, (id, count) -> {
            if (count == null) {
                container.addMessageListener(this, new ChannelTopic(roomChannel(id)));
                return 1;
            }
            return count + 1;
        });
    }

    // 이 서버의 세션 하나가 그 방을 떠남. 마지막 세션이면 구독 해제
    public void unwatch(long roomId) {
        localWatchers.computeIfPresent(roomId, (id, count) -> {
            if (count > 1) {
                return count - 1;
            }
            container.removeMessageListener(this, new ChannelTopic(roomChannel(id)));
            return null;
        });
    }

    // 본문을 다시 객체로 만들지 않고 JSON 트리째 넘김. 형식을 몰라도 그대로 나감
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            JsonNode envelope = objectMapper.readTree(message.getBody());
            messaging.convertAndSend(envelope.get("destination").asText(), envelope.get("payload"));
        } catch (IOException exception) {
            log.warn("토픽 메시지를 읽지 못함: {}", exception.getMessage());
        }
    }

    private static String channelOf(String destination) {
        if (destination.equals(LOBBY_TOPIC)) {
            return LOBBY_CHANNEL;
        }
        if (destination.startsWith(ROOM_TOPIC_PREFIX)) {
            return roomChannel(Long.parseLong(destination.substring(ROOM_TOPIC_PREFIX.length())));
        }
        throw new IllegalArgumentException("서버 간 전파 대상이 아닌 목적지: " + destination);
    }

    private static String roomChannel(long roomId) {
        return "morris:room:" + roomId;
    }

    private record Envelope(String destination, Object payload) {}
}
