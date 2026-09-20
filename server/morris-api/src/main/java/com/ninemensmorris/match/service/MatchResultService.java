package com.ninemensmorris.match.service;

import com.ninemensmorris.core.rating.Rating;
import com.ninemensmorris.core.rating.RatingPolicy;
import com.ninemensmorris.match.domain.Match;
import com.ninemensmorris.match.repository.MatchRepository;
import com.ninemensmorris.user.domain.User;
import com.ninemensmorris.user.domain.User.MatchOutcome;
import com.ninemensmorris.user.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 경기 결과를 MMR 과 전적에 반영
//
// 승자 갱신 / 패자 갱신 / 전적 저장이 한 트랜잭션이어야 한다
// 기존에는 increaseScore 와 decreaseScore 가 각자 별개 트랜잭션이라
// 한쪽만 커밋되어 점수 총합이 어긋날 수 있었다
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchResultService {

    private final UserRepository userRepository;
    private final MatchRepository matchRepository;

    @Transactional
    public void record(long blackId, long whiteId, Long winnerId, String endReason, int moveCount) {
        User black = userRepository.findById(blackId).orElse(null);
        User white = userRepository.findById(whiteId).orElse(null);
        if (black == null || white == null) {
            log.warn("경기 결과를 반영할 수 없음. 사용자 없음 black={} white={}", blackId, whiteId);
            return;
        }

        // 비로그인 계정은 얼마든지 새로 만들 수 있다
        // 레이팅만 막고 전적을 남기면 배치 판수를 공짜로 채워 티어를 받을 수 있으므로
        // 그런 판은 아예 기록하지 않는다. 양쪽 다 잃는 것이 없어야 몰아주기 유인이 생기지 않는다
        if (black.isVisitor() || white.isVisitor()) {
            log.info("비로그인 계정이 낀 판이라 기록하지 않음 black={} white={} 사유={} 수={}", blackId, whiteId, endReason, moveCount);
            return;
        }

        User winner = winnerId == null ? null : (winnerId == blackId ? black : white);
        Match match = Match.of(black, white, winner, endReason, moveCount);

        int blackDelta = 0;
        int whiteDelta = 0;

        // 조기 종료나 반복 대전이면 레이팅을 건드리지 않고 기록만 남긴다
        if (RatingPolicy.isRated(moveCount)) {
            double factor = repeatFactor(blackId, whiteId);
            blackDelta = RatingPolicy.applyFactor(rawDelta(black, white, winner), factor);
            whiteDelta = RatingPolicy.applyFactor(rawDelta(white, black, winner), factor);
            match.recordRating(blackDelta, whiteDelta);
        }

        black.applyMatchResult(blackDelta, outcomeOf(black, winner));
        white.applyMatchResult(whiteDelta, outcomeOf(white, winner));
        matchRepository.save(match);

        log.info(
                "경기 기록 black={}({}) white={}({}) 승자={} 사유={} 수={} 랭크반영={}",
                blackId,
                formatDelta(blackDelta),
                whiteId,
                formatDelta(whiteDelta),
                winnerId,
                endReason,
                moveCount,
                match.isRated());
    }

    private int rawDelta(User me, User opponent, User winner) {
        int myMmr = me.getMmr();
        int opponentMmr = opponent.getMmr();
        int myGames = me.gamesPlayed();

        if (winner == null) {
            return Rating.draw(myMmr, opponentMmr, myGames);
        }
        return winner == me ? Rating.win(myMmr, opponentMmr, myGames) : Rating.loss(myMmr, opponentMmr, myGames);
    }

    private MatchOutcome outcomeOf(User user, User winner) {
        if (winner == null) {
            return MatchOutcome.DRAW;
        }
        return winner == user ? MatchOutcome.WIN : MatchOutcome.LOSS;
    }

    // 같은 상대와 짜고 반복하는 것을 막기 위한 감쇠 계수
    private double repeatFactor(long blackId, long whiteId) {
        Instant since = Instant.now().minus(Duration.ofHours(RatingPolicy.REPEAT_WINDOW_HOURS));
        int playedBefore = matchRepository.countRecentBetween(blackId, whiteId, since);
        return RatingPolicy.repeatOpponentFactor(playedBefore);
    }

    private String formatDelta(int delta) {
        return delta >= 0 ? "+" + delta : String.valueOf(delta);
    }
}
