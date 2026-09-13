package com.ninemensmorris.game.domain;

// 플레이어가 방에서 빠진 이유
// 남은 사람에게 "상대가 끊겼다" 와 "상대가 나갔다" 는 다른 정보라 구분해서 알림
public enum ExitCause {
    DISCONNECT, // 소켓이 끊기고 유예 시간까지 지남
    LEAVE // 스스로 나가기를 누름
}
