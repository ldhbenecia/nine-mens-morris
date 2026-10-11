package com.ninemensmorris.game.dto.response;

import com.ninemensmorris.game.domain.FirstMoveRule;
import com.ninemensmorris.game.domain.Room;
import com.ninemensmorris.user.domain.User;

public record RoomSummaryResponse(
        long roomId,
        String title,
        long hostId,
        String hostNickname,
        String hostImageUrl,
        int hostRating,
        boolean hostVisitor, // 게스트 방장의 MMR 은 움직이지 않는 값이라 화면에서 숨김
        int playerCount,
        boolean playing,
        FirstMoveRule firstMoveRule,
        boolean rated) {

    private static final String UNKNOWN_HOST = "알 수 없음";

    // 방장 정보는 방에 복사해 두지 않고 조회 시점에 채움. 복사하면 점수가 올라도 목록은 옛 값
    // host 가 null 이면 방이 남아 있는 사이 계정이 지워진 경우
    public static RoomSummaryResponse of(Room room, User host, boolean rated) {
        return new RoomSummaryResponse(
                room.roomId(),
                room.title(),
                room.hostId(),
                host == null ? UNKNOWN_HOST : host.getNickname(),
                host == null ? null : host.getImageUrl(),
                host == null ? 0 : host.getMmr(),
                host != null && host.isVisitor(),
                room.playerCount(),
                room.isPlaying(),
                room.firstMoveRule(),
                rated);
    }
}
