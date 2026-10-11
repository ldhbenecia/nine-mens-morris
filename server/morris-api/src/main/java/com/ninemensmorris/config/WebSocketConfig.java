package com.ninemensmorris.config;

import com.ninemensmorris.observability.StompLogContextInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    static final long HEARTBEAT_MILLIS = 10_000;

    private final String[] allowedOrigins;
    private TaskScheduler heartbeatScheduler;
    private final ClientFrameGuard clientFrameGuard;
    private final StompLogContextInterceptor logContextInterceptor;

    public WebSocketConfig(
            @Value("${cors.allowed-origins}") String[] allowedOrigins,
            ClientFrameGuard clientFrameGuard,
            StompLogContextInterceptor logContextInterceptor) {
        this.allowedOrigins = allowedOrigins;
        this.clientFrameGuard = clientFrameGuard;
        this.logContextInterceptor = logContextInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 오리진을 "*" 로 열어두면 악성 사이트가 방문자의 인증 정보로 소켓을 열 수 있음 (CSWSH)
        registry.addEndpoint("/ws").setAllowedOrigins(allowedOrigins);
    }

    // 하트비트에 쓸 스케줄러. 브로커 설정이 이 빈을 만드는 쪽이라 지연 주입해야 순환을 피함
    @Autowired
    public void setHeartbeatScheduler(@Lazy TaskScheduler heartbeatScheduler) {
        this.heartbeatScheduler = heartbeatScheduler;
    }

    // 하트비트가 없으면 네트워크가 끊겨도(FIN 없이) 서버가 몇 분간 소켓이 살아 있다고 봄
    // 그동안 상대 이탈 처리가 안 돼 판이 멈춰 있고 죽은 세션이 메모리를 잡음
    // 서버는 간격의 3배 동안 아무것도 안 오면 세션을 닫음. 10초면 30초 안에 정리됨
    // 클라이언트가 더 짧게 요청해도 협상은 큰 값으로 정해져 모바일 배터리를 덜 씀
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[] {HEARTBEAT_MILLIS, HEARTBEAT_MILLIS})
                .setTaskScheduler(heartbeatScheduler);
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    // 인증·인가를 먼저 걸고 그 결과(주체)를 로그 컨텍스트가 읽는다
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(clientFrameGuard, logContextInterceptor);
    }
}
