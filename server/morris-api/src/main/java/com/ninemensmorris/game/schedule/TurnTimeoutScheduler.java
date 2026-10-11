package com.ninemensmorris.game.schedule;

import com.ninemensmorris.game.service.GameService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 한 수 제한 시간 확인
//
// 판단과 정산은 GameService 가 하고 여기서는 주기와 브로드캐스트만 맡음
// 1초 간격이라 실제로는 제한 시간보다 최대 1초 늦게 끝남. 화면 카운트다운이 0 을 찍은 뒤라 어긋나 보이지 않음
@Component
@RequiredArgsConstructor
public class TurnTimeoutScheduler {

    private static final String ROOM_TOPIC = "/topic/rooms/";

    private final GameService gameService;
    private final SimpMessagingTemplate messaging;

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.SECONDS)
    public void expire() {
        gameService
                .expireTurns()
                .forEach(broadcast -> messaging.convertAndSend(ROOM_TOPIC + broadcast.roomId(), broadcast.event()));
    }
}
