package com.ninemensmorris.game.dto.request;

import com.ninemensmorris.core.board.Board;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// 2·3단계 이동
public record MoveRequest(
        @NotNull @Min(0) @Max(Board.POINTS - 1) Integer from, @NotNull @Min(0) @Max(Board.POINTS - 1) Integer to) {}
