package com.ninemensmorris.game.domain;

// 로비 목록이 바뀜. 방이 생기거나 사라졌거나 로비에 보이는 값이 바뀜
// 바뀌는 경로가 REST·STOMP·스케줄러·소켓 해제로 흩어져 있어 저장소가 판단해 냄
public record LobbyChanged(long roomId) {}
