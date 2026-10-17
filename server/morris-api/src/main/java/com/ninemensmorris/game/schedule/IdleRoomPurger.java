package com.ninemensmorris.game.schedule;

import com.ninemensmorris.game.service.GameService;
import com.ninemensmorris.messaging.ClusterBroadcaster;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 유휴 방 정리 스케줄
//
// 정리 판단과 정산은 GameService 가 하고 여기서는 주기와 브로드캐스트만 맡음
// 서비스가 전송을 알면 안 되므로 전송은 이 바깥 계층에서 함
@Component
@RequiredArgsConstructor
public class IdleRoomPurger {

    private final GameService gameService;
    private final ClusterBroadcaster broadcaster;

    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    public void purge() {
        gameService
                .purgeIdleRooms()
                .forEach(broadcast -> broadcaster.sendToRoom(broadcast.roomId(), broadcast.event()));
    }
}
