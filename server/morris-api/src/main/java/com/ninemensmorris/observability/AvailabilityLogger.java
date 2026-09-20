package com.ninemensmorris.observability;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// SIGTERM 을 받으면 readiness 가 먼저 내려가고 쿠버네티스가 엔드포인트에서 파드를 뺀 뒤
// graceful shutdown 이 남은 요청을 마무리한다
// 이 로그가 없으면 배포 중 끊김이 트래픽 차단 전인지 후인지 구분할 수 없다
@Component
@Slf4j
public class AvailabilityLogger {

    @EventListener
    public void onReadinessChange(AvailabilityChangeEvent<ReadinessState> event) {
        if (event.getState() == ReadinessState.REFUSING_TRAFFIC) {
            log.info("준비 상태 해제. 새 트래픽을 받지 않음");
            return;
        }
        log.info("준비 상태 진입. 트래픽 수신 시작");
    }
}
