package com.ninemensmorris.match.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ninemensmorris.core.rating.Rating;
import com.ninemensmorris.core.rating.RatingPolicy;
import com.ninemensmorris.match.domain.Match;
import com.ninemensmorris.match.repository.MatchRepository;
import com.ninemensmorris.support.IntegrationTestSupport;
import com.ninemensmorris.user.domain.User;
import com.ninemensmorris.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

// 등급 게임 정산은 지금까지 한 번도 실행된 적이 없었다
// E2E 도 단위 테스트도 1수 뒤 기권으로 끝나서 MIN_RATED_MOVES 에 걸려
// isRated 가 false 였고, MMR 을 쓰는 경로 전체가 미검증 상태였다
class MatchResultServiceTest extends IntegrationTestSupport {

    private static final int RATED_MOVES = RatingPolicy.MIN_RATED_MOVES;

    @Autowired
    private MatchResultService matchResultService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MatchRepository matchRepository;

    // 식별자를 DB 가 발급하므로 저장한 뒤에야 알 수 있다
    private long blackId;
    private long whiteId;

    @BeforeEach
    void setUp() {
        // matches 가 users 를 참조하므로 먼저 지운다
        matchRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        List<User> saved =
                userRepository.saveAll(List.of(User.ofKakao("9001", "흑", null), User.ofKakao("9002", "백", null)));
        blackId = saved.get(0).getUserId();
        whiteId = saved.get(1).getUserId();
    }

    private User reload(long userId) {
        return userRepository.findById(userId).orElseThrow();
    }

    private long 상대(String providerId, String nickname) {
        return userRepository.save(User.ofKakao(providerId, nickname, null)).getUserId();
    }

    @Nested
    class 등급_게임 {

        @Test
        @DisplayName("MMR 변동이 제로섬이고 양쪽 전적에 반영된다")
        void 제로섬으로_반영된다() {
            // given — 같은 MMR 로 시작하므로 승자는 오르고 패자는 그만큼 내려가야 함
            int before = reload(blackId).getMmr();

            // when
            matchResultService.record(blackId, whiteId, blackId, "RESIGN", RATED_MOVES);

            // then
            User black = reload(blackId);
            User white = reload(whiteId);
            assertThat(black.getMmr()).isGreaterThan(before);
            assertThat(white.getMmr()).isLessThan(before);
            assertThat(black.getMmr() - before).isEqualTo(before - white.getMmr());
            assertThat(black.getWins()).isEqualTo(1);
            assertThat(white.getLosses()).isEqualTo(1);
        }

        @Test
        @DisplayName("전적 한 행에 변동 전 MMR 과 변동값이 함께 남는다")
        void 원장에_변동값이_남는다() {
            // given
            int blackBefore = reload(blackId).getMmr();
            int whiteBefore = reload(whiteId).getMmr();

            // when
            matchResultService.record(blackId, whiteId, blackId, "RESIGN", RATED_MOVES);

            // then — 이 값이 있어야 K 계수를 바꿔도 지난 경기를 다시 계산할 수 있음
            Match match = matchRepository.findAll().get(0);
            assertThat(match.isRated()).isTrue();
            assertThat(match.getBlackMmrBefore()).isEqualTo(blackBefore);
            assertThat(match.getWhiteMmrBefore()).isEqualTo(whiteBefore);
            assertThat(match.getBlackMmrDelta() + match.getWhiteMmrDelta()).isZero();
            assertThat(reload(blackId).getMmr()).isEqualTo(blackBefore + match.getBlackMmrDelta());
        }

        @Test
        @DisplayName("최고 MMR 은 올랐을 때만 갱신되고 떨어져도 남는다")
        void 최고_기록은_남는다() {
            // given — 흑이 이겨서 최고 기록을 세운다
            matchResultService.record(blackId, whiteId, blackId, "RESIGN", RATED_MOVES);
            int peak = reload(blackId).getPeakMmr();
            assertThat(peak).isEqualTo(reload(blackId).getMmr());

            // when — 이번엔 진다
            matchResultService.record(blackId, whiteId, whiteId, "RESIGN", RATED_MOVES);

            // then
            User black = reload(blackId);
            assertThat(black.getMmr()).isLessThan(peak);
            assertThat(black.getPeakMmr()).isEqualTo(peak);
        }

        @Test
        @DisplayName("무승부는 양쪽 draws 가 오른다")
        void 무승부가_반영된다() {
            // when
            matchResultService.record(blackId, whiteId, null, "DRAW_AGREED", RATED_MOVES);

            // then
            assertThat(reload(blackId).getDraws()).isEqualTo(1);
            assertThat(reload(whiteId).getDraws()).isEqualTo(1);
            assertThat(matchRepository.findAll().get(0).getWinner()).isNull();
        }
    }

    @Nested
    class 어뷰징_대책 {

        @Test
        @DisplayName("일찍 끝난 판은 기록만 남고 MMR 은 그대로다")
        void 조기_종료는_등급에_반영되지_않는다() {
            // given — 붙자마자 기권을 반복해 상대 MMR 을 올려주는 파밍을 막아야 함
            int before = reload(blackId).getMmr();

            // when
            matchResultService.record(blackId, whiteId, blackId, "RESIGN", RATED_MOVES - 1);

            // then
            assertThat(reload(blackId).getMmr()).isEqualTo(before);
            assertThat(reload(whiteId).getMmr()).isEqualTo(before);
            assertThat(reload(blackId).getWins()).isEqualTo(1);
            assertThat(matchRepository.findAll().get(0).isRated()).isFalse();
        }

        @Test
        @DisplayName("같은 상대와 반복하면 변동 폭이 줄어든다")
        void 반복_대전은_감쇠된다() {
            // given — 24시간 윈도 쿼리가 MySQL 에서 실제로 도는지까지 확인
            matchResultService.record(blackId, whiteId, blackId, "RESIGN", RATED_MOVES);
            int firstDelta = matchRepository.findAll().get(0).getBlackMmrDelta();
            matchResultService.record(blackId, whiteId, blackId, "RESIGN", RATED_MOVES);

            // when — 세 번째 판은 앞선 2판 때문에 감쇠 대상
            matchResultService.record(blackId, whiteId, blackId, "RESIGN", RATED_MOVES);

            // then
            List<Match> matches = matchRepository.findAll();
            assertThat(matches).hasSize(3);
            int thirdDelta = matches.get(2).getBlackMmrDelta();
            assertThat(thirdDelta).isPositive().isLessThan(firstDelta);
        }
    }

    @Nested
    class 비로그인_계정 {

        @Test
        @DisplayName("비로그인 계정이 끼면 MMR 도 전적도 원장도 남지 않는다")
        void 비로그인_계정이_끼면_기록하지_않는다() {
            // given — 새 계정을 찍어내 본계정에 점수를 몰아줄 수 있으면 안 된다
            long visitorId = userRepository.save(User.visitor("게스트1234")).getUserId();
            int before = reload(blackId).getMmr();

            // when — 랭크전 조건을 충족하는 길이로 끝낸다
            matchResultService.record(blackId, visitorId, blackId, "RESIGN", RATED_MOVES);

            // then — 이긴 쪽도 얻는 것이 없어야 몰아주기 유인이 사라진다
            User black = reload(blackId);
            assertThat(black.getMmr()).isEqualTo(before);
            assertThat(black.getWins()).isZero();
            assertThat(black.gamesPlayed()).isZero();
            assertThat(matchRepository.findAll()).isEmpty();
        }
    }

    @Nested
    class 배치_기간 {

        @Test
        @DisplayName("배치 중에는 변동 폭이 더 크다")
        void 배치_경기는_변동이_크다() {
            // given — 초반에 제자리를 빨리 찾도록 K 를 크게 씀
            matchResultService.record(blackId, whiteId, blackId, "RESIGN", RATED_MOVES);
            int placementDelta = matchRepository.findAll().get(0).getBlackMmrDelta();

            // when — 배치 판수를 다 채운다. 상대를 바꿔 반복 대전 감쇠를 피함
            for (int i = 0; i < Rating.PLACEMENT_GAMES; i++) {
                long opponent = 상대("9100" + i, "상대" + i);
                matchResultService.record(blackId, opponent, blackId, "RESIGN", RATED_MOVES);
            }
            long lastOpponent = 상대("9200", "마지막");
            matchResultService.record(blackId, lastOpponent, blackId, "RESIGN", RATED_MOVES);

            // then
            List<Match> matches = matchRepository.findAll();
            int settledDelta = matches.get(matches.size() - 1).getBlackMmrDelta();
            assertThat(reload(blackId).gamesPlayed()).isGreaterThan(Rating.PLACEMENT_GAMES);
            assertThat(settledDelta).isLessThan(placementDelta);
        }
    }
}
