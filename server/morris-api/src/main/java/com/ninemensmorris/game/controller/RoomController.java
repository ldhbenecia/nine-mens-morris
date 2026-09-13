package com.ninemensmorris.game.controller;

import com.ninemensmorris.game.command.RoomCommand;
import com.ninemensmorris.game.dto.request.CreateRoomRequest;
import com.ninemensmorris.game.dto.response.CreateRoomResponse;
import com.ninemensmorris.game.dto.response.RoomDetailResponse;
import com.ninemensmorris.game.dto.response.RoomEvent;
import com.ninemensmorris.game.dto.response.RoomEventType;
import com.ninemensmorris.game.dto.response.RoomSummaryResponse;
import com.ninemensmorris.game.service.GameService;
import com.ninemensmorris.game.service.RoomService;
import com.ninemensmorris.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

    // 로비 목록이 바뀌었음을 알리는 토픽. 목록 자체는 REST 로 다시 받아감
    private static final String LOBBY_TOPIC = "/topic/lobby";
    private static final String ROOM_TOPIC = "/topic/rooms/";

    private final RoomService roomService;
    private final GameService gameService;
    private final SimpMessagingTemplate messaging;

    @GetMapping
    public List<RoomSummaryResponse> findAll() {
        return roomService.findAll();
    }

    @GetMapping("/{roomId}")
    public RoomDetailResponse findOne(@PathVariable long roomId) {
        return roomService.findDetail(roomId);
    }

    @PostMapping
    public ResponseEntity<CreateRoomResponse> create(
            AuthenticatedUser actor, @Valid @RequestBody CreateRoomRequest request) {
        CreateRoomResponse response = roomService.create(new RoomCommand.CreateRoom(actor.id(), request.title()));
        notifyLobby();
        return ResponseEntity.created(URI.create("/api/v1/rooms/" + response.roomId()))
                .body(response);
    }

    // 입장은 "방에 플레이어를 추가"하는 것이므로 하위 리소스 생성이다
    // 기존에는 userId 를 본문으로 받아 남의 계정으로 입장시킬 수 있었다
    @PostMapping("/{roomId}/players")
    public ResponseEntity<Void> join(AuthenticatedUser actor, @PathVariable long roomId) {
        roomService.join(new RoomCommand.JoinRoom(actor.id(), roomId));
        // 방장이 새로고침하지 않아도 상대가 들어온 걸 알 수 있어야 함
        messaging.convertAndSend(ROOM_TOPIC + roomId, RoomEvent.by(RoomEventType.PLAYER_JOINED, actor.id()));
        notifyLobby();
        return ResponseEntity.status(201).build();
    }

    // 나가기는 게임 정산까지 얽히므로 GameService 가 처리함
    // 서비스가 이벤트를 돌려줬을 때만 브로드캐스트함
    // 무조건 쏘면 방에 속하지도 않은 사람이 남의 방에 PLAYER_LEFT 를 꽂을 수 있다
    @DeleteMapping("/{roomId}/players/me")
    public ResponseEntity<Void> leave(AuthenticatedUser actor, @PathVariable long roomId) {
        gameService.leave(new RoomCommand.LeaveRoom(actor.id(), roomId)).ifPresent(broadcast -> {
            messaging.convertAndSend(ROOM_TOPIC + broadcast.roomId(), broadcast.event());
            notifyLobby();
        });
        return ResponseEntity.noContent().build();
    }

    private void notifyLobby() {
        messaging.convertAndSend(LOBBY_TOPIC, RoomEvent.signal(RoomEventType.LOBBY_CHANGED));
    }
}
