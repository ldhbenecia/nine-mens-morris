package com.ninemensmorris.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

// 실제 STOMP 연결을 여는 테스트용 클라이언트. 서버 포트를 골라 붙을 수 있음
public final class StompTestClient implements AutoCloseable {

    public static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebSocketStompClient client;

    public StompTestClient() {
        this.client = new WebSocketStompClient(new StandardWebSocketClient());
        this.client.setMessageConverter(new MappingJackson2MessageConverter());
    }

    // 토큰은 CONNECT 프레임 헤더로 보냄. 브라우저와 같은 방식
    public StompSession connect(int port, String bearer) throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add(HttpHeaders.AUTHORIZATION, bearer);
        return client.connectAsync(
                        "ws://localhost:" + port + "/ws",
                        new WebSocketHttpHeaders(),
                        connectHeaders,
                        new StompSessionHandlerAdapter() {})
                .get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
    }

    public static <T> BlockingQueue<T> subscribe(StompSession session, String destination, Class<T> type) {
        BlockingQueue<T> received = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return type;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.add(type.cast(payload));
            }
        });
        return received;
    }

    public static <T> T next(BlockingQueue<T> queue) throws InterruptedException {
        T event = queue.poll(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        assertThat(event).as("메시지가 도착하지 않음").isNotNull();
        return event;
    }

    @Override
    public void close() {
        client.stop();
    }
}
