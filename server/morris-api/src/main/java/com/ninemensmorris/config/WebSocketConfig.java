package com.ninemensmorris.config;

import com.ninemensmorris.observability.StompLogContextInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final String[] allowedOrigins;
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

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    // 인증·인가를 먼저 걸고 그 결과(주체)를 로그 컨텍스트가 읽는다
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(clientFrameGuard, logContextInterceptor);
    }
}
