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
        boolean playing,
        boolean hasGame, // 끝난 판도 true. playing 만 보면 종료된 판 새로고침 때 대기실이 뜸
        boolean rated) { // 랭크전 여부. 비로그인 사용자가 들어오는 순간 꺼진다

    public static RoomDetailResponse of(Room room, String hostNickname, String guestNickname, boolean rated) {
        return new RoomDetailResponse(
                room.roomId(),
                room.title(),
                room.hostId(),
                hostNickname,
                room.guestId(),
                guestNickname,
                room.firstMoveRule(),
                room.isPlaying(),
                room.hasGame(),
                rated);
    }
}
