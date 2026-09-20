package com.ninemensmorris.observability;

import com.ninemensmorris.core.game.EndReason;
import com.ninemensmorris.core.move.RejectReason;
import com.ninemensmorris.game.domain.RoomRegistry;
import com.ninemensmorris.game.domain.SessionTracker;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import org.springframework.stereotype.Component;

// 게임 도메인 메트릭. JVM·HikariCP 는 Actuator 가 알아서 준다
// 개별 착수는 계측하지 않는다. 인메모리 연산이라 값이 의미 없고 수만 늘어난다
@Component
public class GameMetrics {

    // 우상향하면 끝난 방이 정리되지 않고 있다는 뜻
    private static final String ROOMS_ACTIVE = "morris.rooms.active";

    // 사용자 수가 아니라 소켓 수
    private static final String SOCKETS_OPEN = "morris.sockets.open";

    private static final String GAMES_FINISHED = "morris.games.finished";

    // 오르면 프론트와 서버의 규칙이 어긋났거나 조작 시도다. 알림을 걸 대상
    private static final String MOVES_REJECTED = "morris.moves.rejected";

    // 방 생성부터 판 시작까지. 이 서비스의 매칭 대기 시간
    private static final String ROOM_WAIT = "morris.room.wait";

    private static final String TAG_REASON = "reason";

    private final MeterRegistry registry;

    public GameMetrics(MeterRegistry registry, RoomRegistry rooms, SessionTracker sessions) {
        this.registry = registry;

        // 증감을 직접 추적하면 누락된 감소 하나가 영원히 틀린 값을 만든다. 수집 시점에 읽어 간다
        Gauge.builder(ROOMS_ACTIVE, rooms, RoomRegistry::size).register(registry);
        Gauge.builder(SOCKETS_OPEN, sessions, SessionTracker::openSessions).register(registry);
    }

    public void gameFinished(EndReason reason) {
        registry.counter(GAMES_FINISHED, TAG_REASON, reason.name()).increment();
    }

    public void moveRejected(RejectReason reason) {
        registry.counter(MOVES_REJECTED, TAG_REASON, reason.name()).increment();
    }

    public void recordRoomWait(Duration waited) {
        registry.timer(ROOM_WAIT).record(waited);
    }
}
