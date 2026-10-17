package com.ninemensmorris.game.dto.response;

import com.ninemensmorris.core.board.Board;
import com.ninemensmorris.core.board.Stone;
import com.ninemensmorris.core.game.GameStatus;
import com.ninemensmorris.core.game.MorrisGame;
import com.ninemensmorris.core.game.Phase;
import com.ninemensmorris.game.domain.Room;
import java.time.Duration;
import java.time.Instant;

// 판 상태 전체. 클라이언트는 이것만 보고 화면을 그림
//
// 방장/참가자가 아니라 흑/백 기준. 방장이 흑이라는 전제를 두면 선공 방식을 바꿀 수 없음
public record GameStateResponse(
        String[] board,
        long blackId,
        long whiteId,
        long currentTurnId,
        int blackInHand,
        int whiteInHand,
        int blackOnBoard,
        int whiteOnBoard,
        Phase blackPhase,
        Phase whitePhase,
        boolean awaitingRemoval,
        GameStatus status,
        Long winnerId,
        Long loserId,
        String endReason,
        // 차례인 쪽에 남은 시간. 판이 끝났으면 null
        // 마감 시각이 아니라 남은 양을 줌. 클라이언트 시계가 서버와 어긋나도 받은 순간부터 세면 맞음
        Long turnRemainingMillis,
        // 방이 바뀔 때마다 오르는 번호. 서버 여러 대가 보낸 이벤트가 뒤바뀌어 와도 클라이언트가 낡은 것을 버림
        long revision) {

    public static GameStateResponse of(Room room, MorrisGame game, Instant now) {
        Board board = game.board();
        String[] cells = new String[Board.POINTS];
        for (int point = 0; point < Board.POINTS; point++) {
            Stone stone = board.stoneAt(point);
            cells[point] = stone == null ? "EMPTY" : stone.name();
        }

        Long winnerId = null;
        Long loserId = null;
        String endReason = null;
        if (game.outcome() != null) {
            endReason = game.outcome().reason().name();
            Stone winner = game.outcome().winner();
            if (winner != null) {
                winnerId = room.userIdOf(winner);
                loserId = room.userIdOf(winner.opponent());
            }
        }

        return new GameStateResponse(
                cells,
                room.blackId(),
                room.whiteId(),
                room.userIdOf(game.currentTurn()),
                game.inHand(Stone.BLACK),
                game.inHand(Stone.WHITE),
                game.onBoard(Stone.BLACK),
                game.onBoard(Stone.WHITE),
                game.phaseOf(Stone.BLACK),
                game.phaseOf(Stone.WHITE),
                game.awaitingRemoval(),
                game.status(),
                winnerId,
                loserId,
                endReason,
                remainingMillis(room, game, now),
                room.revision());
    }

    private static Long remainingMillis(Room room, MorrisGame game, Instant now) {
        if (game.status() != GameStatus.PLAYING || room.turnDeadline() == null) {
            return null;
        }
        return Math.max(0, Duration.between(now, room.turnDeadline()).toMillis());
    }
}
