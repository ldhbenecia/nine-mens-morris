package com.ninemensmorris.core.game;

import com.ninemensmorris.core.board.Stone;
import java.util.Map;

// 판 상태 전부를 담은 값. 저장소에 넣었다가 MorrisGame.restore 로 그대로 되살림
// 화면에 안 보이는 상태(반복 기록·제안 횟수)까지 넣어야 되살린 판의 규칙이 같음
public record GameSnapshot(
        String points, // 지점 0~23 순서로 B / W / . 한 글자씩
        Stone currentTurn,
        GameStatus status,
        Outcome outcome,
        int blackInHand,
        int whiteInHand,
        int blackDrawOffers,
        int whiteDrawOffers,
        Map<String, Integer> seenPositions,
        boolean awaitingRemoval,
        Stone drawOfferedBy,
        int movesSinceRemoval,
        int totalMoves) {

    public GameSnapshot {
        seenPositions = Map.copyOf(seenPositions);
    }
}
