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
import com.ninemensmorris.messaging.ClusterBroadcaster;
import com.ninemensmorris.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    private final RoomService roomService;
    private final GameService gameService;
    private final ClusterBroadcaster broadcaster;

    @GetMapping
    public List<RoomSummaryResponse> findAll() {
        return roomService.findAll();
    }

    @GetMapping("/{roomId}")
    public RoomDetailResponse findOne(AuthenticatedUser actor, @PathVariable long roomId) {
        return roomService.findDetail(roomId, actor.id());
    }

    @PostMapping
    public ResponseEntity<CreateRoomResponse> create(
            AuthenticatedUser actor, @Valid @RequestBody CreateRoomRequest request) {
        CreateRoomResponse response = roomService.create(new RoomCommand.CreateRoom(actor.id(), request.title()));
        return ResponseEntity.created(URI.create("/api/v1/rooms/" + response.roomId()))
                .body(response);
    }

    @PostMapping("/{roomId}/players")
    public ResponseEntity<Void> join(AuthenticatedUser actor, @PathVariable long roomId) {
        roomService.join(new RoomCommand.JoinRoom(actor.id(), roomId));
        // 방장이 새로고침하지 않아도 상대가 들어온 걸 알 수 있어야 함
        broadcaster.sendToRoom(roomId, RoomEvent.by(RoomEventType.PLAYER_JOINED, actor.id()));
        return ResponseEntity.status(201).build();
    }

    @DeleteMapping("/{roomId}/players/me")
    public ResponseEntity<Void> leave(AuthenticatedUser actor, @PathVariable long roomId) {
        gameService
                .leave(new RoomCommand.LeaveRoom(actor.id(), roomId))
                .ifPresent(broadcast -> broadcaster.sendToRoom(broadcast.roomId(), broadcast.event()));
        return ResponseEntity.noContent().build();
    }
}
