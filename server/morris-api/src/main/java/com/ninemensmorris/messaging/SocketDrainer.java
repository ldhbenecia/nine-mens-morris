package com.ninemensmorris.messaging;

import com.ninemensmorris.game.domain.SessionTracker;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

// 서버가 내려가기 전에 이 서버에 붙은 세션을 다른 서버로 먼저 옮김
//
// 소켓은 이 서버와 맺은 연결이라 서버가 내려가면 끊길 수밖에 없음
// 예고 없이 끊으면 화면에 끊김이 보이고 다시 붙기 전까지 수를 둘 수 없음
// 내려가기 전에 "옮겨라" 를 보내면 클라이언트가 다른 서버에 새 연결을 먼저 붙인 뒤 이 연결을 닫음
// 판은 Redis 에 있어 두 연결이 잠깐 겹쳐도 되고, 옮기는 사이 이 연결로 온 수도 그대로 처리됨
//
// ContextClosedEvent 는 브로커·웹소켓 핸들러가 멈추는 생명주기 종료보다 먼저 옴. 기다리는 동안 이 서버는 아직 다 돌아감
// 그보다 먼저 준비 상태가 내려가므로 새 연결은 다른 서버로 감
@Component
@RequiredArgsConstructor
@Slf4j
public class SocketDrainer {

    // 이 안에 옮기지 않은 세션은 끊고 내려감. 신호를 모르는 옛 클라이언트도 조용한 재연결로 이어 붙음
    private static final Duration DRAIN_TIMEOUT = Duration.ofSeconds(10);
    private static final long POLL_MILLIS = 100;

    private final SimpMessagingTemplate messaging;
    private final SessionTracker sessions;

    @EventListener(ContextClosedEvent.class)
    public void onShutdown() {
        drain(DRAIN_TIMEOUT);
    }

    // 클러스터 브로드캐스터를 거치지 않음. 이 서버 브로커의 구독자, 곧 이 서버에 붙은 세션에게만 감
    void drain(Duration timeout) {
        int open = sessions.openSessions();
        if (open == 0) {
            return;
        }
        log.info("종료 전 세션 이동 요청 sessions={}", open);
        messaging.convertAndSend(StompDestinations.SYSTEM_TOPIC, ServerNotice.RECONNECT);

        long deadline = System.nanoTime() + timeout.toNanos();
        while (sessions.openSessions() > 0 && System.nanoTime() < deadline) {
            try {
                Thread.sleep(POLL_MILLIS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.info("세션 이동 대기 끝. 남은 sessions={}", sessions.openSessions());
    }
}
