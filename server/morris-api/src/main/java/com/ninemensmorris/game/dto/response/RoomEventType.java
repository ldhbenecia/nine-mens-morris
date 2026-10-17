package com.ninemensmorris.game.dto.response;

// 방 토픽 하나로 흐르는 이벤트 종류
public enum RoomEventType {
    // 로비 목록이 바뀜. 목록 자체는 REST 로 다시 받아감
    LOBBY_CHANGED,
    PLAYER_JOINED,
    PLAYER_LEFT,
    SETTINGS_CHANGED, // 선공 방식 변경
    STARTED,
    STATE_CHANGED, // 착수 결과
    FINISHED, // 승패 또는 무승부 확정
    DRAW_OFFERED,
    DRAW_DECLINED,
    SNAPSHOT, // 재접속 시 현재 판 전체
    OPPONENT_DISCONNECTED
}
