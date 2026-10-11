package com.ninemensmorris.game.domain;

import com.ninemensmorris.core.game.GameSnapshot;
import java.time.Instant;

// 저장소에 넣는 방 하나. 판이 시작되기 전이면 game 이 null
public record RoomSnapshot(
        long roomId,
        String title,
        long hostId,
        Instant createdAt,
        Long guestId,
        FirstMoveRule firstMoveRule,
        long blackId,
        long whiteId,
        Instant lastActivityAt,
        Instant turnDeadline,
        GameSnapshot game) {}
