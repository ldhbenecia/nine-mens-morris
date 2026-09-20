package com.ninemensmorris.common.response;

import lombok.Getter;

@Getter
public enum ErrorCode {
    USER_NOT_FOUND(404, "해당 사용자를 찾을 수 없습니다."),
    UNAUTHORIZED(401, "로그인이 필요합니다."),
    INVALID_REQUEST(400, "요청 형식이 올바르지 않습니다."),
    TOO_MANY_REQUESTS(429, "요청이 너무 잦습니다. 잠시 후 다시 시도해 주세요."),
    INTERNAL_ERROR(500, "처리 중 오류가 발생했습니다."),

    // Room
    ROOM_NOT_FOUND(404, "방을 찾을 수 없습니다."),
    ROOM_FULL(400, "방이 가득 찼습니다."),
    ALREADY_JOINED(400, "이미 참가한 방입니다."),
    NOT_ROOM_HOST(403, "방장만 할 수 있습니다."),
    NOT_ENOUGH_PLAYERS(400, "두 명이 모여야 시작할 수 있습니다."),
    GAME_ALREADY_STARTED(400, "이미 시작된 게임입니다.");

    private final int status;
    private final String message;

    ErrorCode(final int status, final String message) {
        this.status = status;
        this.message = message;
    }
}
