package com.ninemensmorris.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ninemensmorris.match.repository.MatchRepository;
import com.ninemensmorris.support.IntegrationTestSupport;
import com.ninemensmorris.user.domain.User;
import com.ninemensmorris.user.domain.User.MatchOutcome;
import com.ninemensmorris.user.dto.response.RankingResponse;
import com.ninemensmorris.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserServiceTest extends IntegrationTestSupport {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MatchRepository matchRepository;

    @BeforeEach
    void setUp() {
        matchRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    private void 사용자(long userId, int mmr) {
        User user = User.ofKakao(userId, userId + "@test.com", "u" + userId, null);
        // 초기값에서 목표 MMR 까지 한 번에 이동
        user.applyMatchResult(mmr - user.getMmr(), MatchOutcome.DRAW);
        userRepository.save(user);
    }

    @Test
    @DisplayName("동점자는 같은 등수를 받고 다음 사람은 인원수만큼 건너뛴다")
    void 동점자는_같은_등수다() {
        // given
        사용자(1L, 1200);
        사용자(2L, 1100);
        사용자(3L, 1100);
        사용자(4L, 1000);

        // when
        List<RankingResponse> rankings = userService.findRankings(10);

        // then
        assertThat(rankings).extracting(RankingResponse::rank).containsExactly(1, 2, 2, 4);
    }

    @Test
    @DisplayName("목록의 등수와 내 프로필의 등수가 일치한다")
    void 두_경로의_등수가_일치한다() {
        // given — 목록은 인덱스로, 프로필은 count(mmr>) 로 세고 있어 동점에서 어긋났다
        사용자(1L, 1200);
        사용자(2L, 1100);
        사용자(3L, 1100);
        사용자(4L, 1000);

        // when
        List<RankingResponse> rankings = userService.findRankings(10);

        // then
        for (RankingResponse entry : rankings) {
            assertThat(userService.findMe(entry.userId()).rank()).isEqualTo(entry.rank());
        }
    }

    @Test
    @DisplayName("동점자 순서가 호출마다 바뀌지 않는다")
    void 동점자_순서가_고정된다() {
        // given — 정렬에 타이브레이커가 없으면 순서가 비결정적이다
        for (long userId = 1L; userId <= 8L; userId++) {
            사용자(userId, 1000);
        }

        // when
        List<Long> first = userService.findRankings(10).stream()
                .map(RankingResponse::userId)
                .toList();
        List<Long> second = userService.findRankings(10).stream()
                .map(RankingResponse::userId)
                .toList();

        // then
        assertThat(first).isEqualTo(second).isSorted();
    }
}
