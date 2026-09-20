package com.ninemensmorris.observability;

import com.ninemensmorris.game.domain.RoomRegistry;
import com.ninemensmorris.game.domain.SessionTracker;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

// 판단 기준은 스케줄러 생존 하나다
// 스케줄러가 죽으면 재접속 유예와 유휴 방 정리가 멈춰 끝나지 않는 판이 쌓인다
// 프로세스는 멀쩡해 보이지만 게임을 끝낼 수 없는 상태라 트래픽을 더 받으면 안 된다
//
// 방이 많다는 이유로는 DOWN 으로 두지 않는다. 누수가 나면 전 파드가 동시에 빠져 서비스가 멈춘다
// 누수는 morris.rooms.active 로 본다
@Component
@RequiredArgsConstructor
public class GameSessionHealthIndicator implements HealthIndicator {

    private final RoomRegistry rooms;
    private final SessionTracker sessions;
    private final TaskScheduler taskScheduler;

    @Override
    public Health health() {
        Health.Builder health = schedulerAlive() ? Health.up() : Health.down().withDetail("scheduler", "종료됨");
        return health.withDetail("activeRooms", rooms.size())
                .withDetail("openSessions", sessions.openSessions())
                .build();
    }

    private boolean schedulerAlive() {
        if (!(taskScheduler instanceof ThreadPoolTaskScheduler pool)) {
            return true;
        }
        try {
            return !pool.getScheduledThreadPoolExecutor().isShutdown();
        } catch (IllegalStateException notInitialized) {
            return false;
        }
    }
}
