package com.ninemensmorris.game.dto.response;

import com.ninemensmorris.game.domain.FirstMoveRule;
import com.ninemensmorris.game.domain.Room;

// 게임 시작 전에도 필요한 방 정보
//
// GameStateResponse 는 판이 시작돼야 채워지므로 그것만으론
// 내가 방장인지 상대가 들어왔는지 알 수 없음
public record RoomDetailResponse(
        long roomId,
        String title,
        long hostId,
        String hostNickname,
        Long guestId,
        String guestNickname,
        FirstMoveRule firstMoveRule,
        boolean playing) {

    public static RoomDetailResponse of(Room room, String hostNickname, String guestNickname) {
        return new RoomDetailResponse(
                room.roomId(),
                room.title(),
                room.hostId(),
                hostNickname,
                room.guestId(),
                guestNickname,
                room.firstMoveRule(),
                room.isPlaying());
    }
}
