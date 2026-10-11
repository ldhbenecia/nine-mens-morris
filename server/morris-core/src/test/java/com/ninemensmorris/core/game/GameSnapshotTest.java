package com.ninemensmorris.core.game;

import static org.assertj.core.api.Assertions.assertThat;

import com.ninemensmorris.core.board.Board;
import com.ninemensmorris.core.board.Stone;
import com.ninemensmorris.core.move.Move;
import com.ninemensmorris.core.move.MoveResult;
import com.ninemensmorris.core.move.RejectReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// 서버가 바뀌어도 판이 이어지려면 저장했다 되살린 판이 원래 판과 구별되지 않아야 함
// 화면에 안 보이는 상태(반복 기록·무승부 제안 횟수)가 빠지면 규칙이 조용히 달라짐
class GameSnapshotTest {

    private static MorrisGame restored(MorrisGame game) {
        return MorrisGame.restore(game.snapshot());
    }

    @Test
    @DisplayName("되살린 판에 같은 수를 두면 원래 판과 같은 상태가 된다")
    void 되살려도_같은_판이다() {
        // given — 3연속 직후 제거 대기
        MorrisGame original = new MorrisGame(Stone.BLACK);
        original.apply(Stone.BLACK, new Move.Place(0));
        original.apply(Stone.WHITE, new Move.Place(9));
        original.apply(Stone.BLACK, new Move.Place(1));
        original.apply(Stone.WHITE, new Move.Place(10));
        original.apply(Stone.BLACK, new Move.Place(2));
        MorrisGame copy = restored(original);

        // when
        original.apply(Stone.BLACK, new Move.Remove(9));
        copy.apply(Stone.BLACK, new Move.Remove(9));

        // then
        assertThat(copy.snapshot()).isEqualTo(original.snapshot());
        assertThat(copy.currentTurn()).isEqualTo(Stone.WHITE);
    }

    @Test
    @DisplayName("반복 기록이 이어져 되살린 뒤 세 번째 반복에서 무승부가 난다")
    void 반복_기록이_이어진다() {
        // given — 한 번 왕복해 같은 국면이 두 번 나온 상태에서 저장
        MorrisGame game = new MorrisGame(
                Board.parse(
                        """
                        B  .  .
                         B . .
                          B..
                        ... ...
                          W..
                         W . .
                        W  .  .
                        """),
                Stone.BLACK,
                0,
                0);
        game.apply(Stone.BLACK, new Move.Slide(0, 1));
        game.apply(Stone.WHITE, new Move.Slide(21, 22));
        game.apply(Stone.BLACK, new Move.Slide(1, 0));
        game.apply(Stone.WHITE, new Move.Slide(22, 21));
        MorrisGame copy = restored(game);

        // when — 되살린 판에서 한 번 더 왕복
        copy.apply(Stone.BLACK, new Move.Slide(0, 1));
        copy.apply(Stone.WHITE, new Move.Slide(21, 22));
        copy.apply(Stone.BLACK, new Move.Slide(1, 0));
        MoveResult result = copy.apply(Stone.WHITE, new Move.Slide(22, 21));

        // then
        assertThat(result).isInstanceOf(MoveResult.Finished.class);
        assertThat(copy.outcome().reason()).isEqualTo(EndReason.THREEFOLD_REPETITION);
    }

    @Test
    @DisplayName("무승부 제안 횟수와 걸려 있는 제안이 이어진다")
    void 무승부_제안이_이어진다() {
        // given — 흑이 제안 한도를 다 쓰고, 마지막 제안이 걸려 있음
        MorrisGame game = new MorrisGame(Stone.BLACK);
        for (int i = 0; i < 3; i++) {
            game.apply(Stone.BLACK, new Move.OfferDraw());
            if (i < 2) {
                game.apply(Stone.WHITE, new Move.RespondDraw(false));
            }
        }
        MorrisGame copy = restored(game);

        // then — 걸린 제안은 수락할 수 있고, 새 제안은 한도에 걸림
        assertThat(copy.drawOfferedBy()).isEqualTo(Stone.BLACK);
        copy.apply(Stone.WHITE, new Move.RespondDraw(false));
        MoveResult again = copy.apply(Stone.BLACK, new Move.OfferDraw());
        assertThat(((MoveResult.Rejected) again).reason()).isEqualTo(RejectReason.DRAW_OFFER_LIMIT);
    }

    @Test
    @DisplayName("끝난 판은 결과까지 되살아난다")
    void 끝난_판도_되살아난다() {
        // given
        MorrisGame game = new MorrisGame(Stone.BLACK);
        game.apply(Stone.BLACK, new Move.Resign());

        // when
        MorrisGame copy = restored(game);

        // then
        assertThat(copy.status()).isEqualTo(GameStatus.FINISHED);
        assertThat(copy.outcome()).isEqualTo(Outcome.win(Stone.WHITE, EndReason.RESIGN));
    }
}
