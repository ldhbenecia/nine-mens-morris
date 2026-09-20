package com.ninemensmorris.observability;

import com.ninemensmorris.common.logging.LogContext;
import org.springframework.boot.task.ThreadPoolTaskSchedulerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 스케줄러에서 도는 것이 잡무가 아니라 게임 정산이다. 그 로그를 묶으려면 traceId 가 필요하다
// 감싸는 대상이 실행 시점의 Runnable 이라 @Scheduled 는 회차마다 새 값을 받는다
@Configuration
public class SchedulerLogContextConfig {

    @Bean
    ThreadPoolTaskSchedulerCustomizer schedulerLogContextCustomizer() {
        return scheduler -> scheduler.setTaskDecorator(task -> () -> {
            LogContext.startTrace();
            try {
                task.run();
            } finally {
                LogContext.clear();
            }
        });
    }
}
