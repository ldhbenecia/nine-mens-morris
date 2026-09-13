package com.ninemensmorris.game.dto.request;

import com.ninemensmorris.core.board.Board;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// 밀을 만든 뒤 상대 돌 제거
public record RemoveRequest(@NotNull @Min(0) @Max(Board.POINTS - 1) Integer at) {}
