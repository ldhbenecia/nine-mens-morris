package com.ninemensmorris.game.dto.request;

import com.ninemensmorris.core.board.Board;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// 1단계 배치. gameId 도 userId 도 없다 — roomId 는 목적지 경로에서, 행위자는 Principal 에서 온다
//
// int 로 받으면 필드가 빠진 요청이 조용히 0 이 되어 "0번 지점 착수" 로 처리된다
// 잘못된 요청이 합법적인 수가 되면 안 되므로 Integer + @NotNull 로 받음
public record PlaceRequest(@NotNull @Min(0) @Max(Board.POINTS - 1) Integer to) {}
