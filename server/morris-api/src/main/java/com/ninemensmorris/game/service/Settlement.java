package com.ninemensmorris.game.service;

import com.ninemensmorris.core.game.MorrisGame;
import com.ninemensmorris.core.game.Outcome;
import com.ninemensmorris.game.domain.Room;

// 끝난 판을 DB 에 기록할 때 필요한 값만 복사해 둔 것
// 방 락 안에서 만들어야 함. Room 을 그대로 들고 나가면 락을 놓은 사이 새 판이 시작돼 흑/백이 바뀔 수 있음
record Settlement(long blackId, long whiteId, Long winnerId, Outcome outcome, int moves) {

    static Settlement of(Room room, MorrisGame game, Outcome outcome) {
        Long winnerId = outcome.isDraw() ? null : room.userIdOf(outcome.winner());
        return new Settlement(room.blackId(), room.whiteId(), winnerId, outcome, game.totalMoves());
    }
}
