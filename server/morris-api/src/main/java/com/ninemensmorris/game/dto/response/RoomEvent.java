package com.ninemensmorris.game.dto.response;

// 방 토픽으로 나가는 메시지. state 는 판이 있을 때만 채워짐
public record RoomEvent(RoomEventType type, GameStateResponse state, Long actorId) {

    public static RoomEvent of(RoomEventType type, GameStateResponse state) {
        return new RoomEvent(type, state, null);
    }

    public static RoomEvent by(RoomEventType type, long actorId) {
        return new RoomEvent(type, null, actorId);
    }

    // 특정 사용자의 행동이 아닌 알림. actorId 에 0 같은 값을 넣으면 0번 사용자가 한 일처럼 보임
    public static RoomEvent signal(RoomEventType type) {
        return new RoomEvent(type, null, null);
    }
}
