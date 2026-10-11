package com.ninemensmorris.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

// 방·로비 토픽을 모든 서버로 보냄
//
// 서버마다 SimpleBroker 가 자기에게 붙은 세션만 알아서 한 서버에서 보내면 다른 서버에 붙은 상대는 못 받음
// Redis 채널로 내보내고 모든 서버(보낸 서버 포함)가 받아 자기 브로커로 흘림
// 보낸 서버도 채널을 거쳐 받으므로 자기 브로커로 직접 보내지 않음. 직접 보내면 두 번 감
//
// pub/sub 은 전달을 보장하지 않음. 받는 서버가 그 순간 죽어 있으면 유실되지만
// 그 서버에 붙어 있던 클라이언트는 다른 서버로 다시 붙으며 sync 로 판 전체를 받음
//
// 개인 큐(/user/queue/*)는 여기로 보내지 않음. 그 프레임을 보낸 세션이 이 서버에 있음
@Component
@RequiredArgsConstructor
@Slf4j
public class ClusterBroadcaster implements MessageListener {

    public static final String CHANNEL = "morris:topics";

    private final StringRedisTemplate redis;
    private final SimpMessagingTemplate messaging;
    private final ObjectMapper objectMapper;

    public void send(String destination, Object payload) {
        try {
            String envelope = objectMapper.writeValueAsString(new Envelope(destination, payload));
            redis.convertAndSend(CHANNEL, envelope);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("토픽 메시지 직렬화 실패 destination=" + destination, exception);
        }
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

    private record Envelope(String destination, Object payload) {}
}
