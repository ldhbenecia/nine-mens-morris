package com.ninemensmorris.config;

import org.springframework.boot.task.ThreadPoolTaskSchedulerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

// 애플리케이션 전용 스케줄러
//
// 이 빈이 없으면 TaskScheduler 가 messageBrokerTaskScheduler 하나뿐이라
// Boot 자동 설정이 물러나고, @Scheduled 와 재접속 유예가 STOMP 하트비트 스케줄러를 같이 쓴다
// 정산이 DB 를 기다리는 동안 하트비트가 밀리면 멀쩡한 연결이 끊긴다
// (그때는 spring.task.scheduling.* 도 적용되지 않는다)
@Configuration
public class SchedulingConfig {

    @Bean
    @Primary
    ThreadPoolTaskScheduler taskScheduler(ThreadPoolTaskSchedulerBuilder builder) {
        return builder.build();
    }
}
