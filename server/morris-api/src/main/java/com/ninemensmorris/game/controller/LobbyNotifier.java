package com.ninemensmorris.game.controller;

import com.ninemensmorris.game.domain.LobbyChanged;
import com.ninemensmorris.game.dto.response.RoomEvent;
import com.ninemensmorris.game.dto.response.RoomEventType;
import com.ninemensmorris.messaging.ClusterBroadcaster;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// 로비 화면에 목록을 다시 받으라고 알림. 목록 자체는 REST 로 받아감
@Component
@RequiredArgsConstructor
public class LobbyNotifier {

    private final ClusterBroadcaster broadcaster;

    @EventListener
    public void onLobbyChanged(LobbyChanged event) {
        broadcaster.sendToLobby(RoomEvent.signal(RoomEventType.LOBBY_CHANGED));
    }
}
