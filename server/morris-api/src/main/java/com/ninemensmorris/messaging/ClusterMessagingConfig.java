package com.ninemensmorris.messaging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class ClusterMessagingConfig {

    // 구독 전용 연결을 하나 따로 잡음. 구독 중인 연결로는 다른 명령을 보낼 수 없음
    @Bean
    public RedisMessageListenerContainer topicListenerContainer(
            RedisConnectionFactory connectionFactory, ClusterBroadcaster broadcaster) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(broadcaster, new ChannelTopic(ClusterBroadcaster.CHANNEL));
        return container;
    }
}
