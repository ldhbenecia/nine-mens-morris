package com.ninemensmorris.config;

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

    public WebSocketConfig(
            @Value("${cors.allowed-origins}") String[] allowedOrigins, ClientFrameGuard clientFrameGuard) {
        this.allowedOrigins = allowedOrigins;
        this.clientFrameGuard = clientFrameGuard;
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

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(clientFrameGuard);
    }
}
