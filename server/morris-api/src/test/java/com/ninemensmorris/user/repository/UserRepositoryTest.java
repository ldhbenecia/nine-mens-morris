package com.ninemensmorris.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ninemensmorris.match.repository.MatchRepository;
import com.ninemensmorris.support.IntegrationTestSupport;
import com.ninemensmorris.user.domain.Provider;
import com.ninemensmorris.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

// 카카오 회원번호를 기본키로 쓰던 구조를 대체키로 바꿨다
// 같은 계정이 두 행이 되는 것을 이제 PK 가 아니라 유니크 제약이 막으므로
// 그 제약이 실제 MySQL 에서 도는지 확인해야 한다
class UserRepositoryTest extends IntegrationTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MatchRepository matchRepository;

    @BeforeEach
    void setUp() {
        matchRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("카카오 회원번호로 기존 계정을 찾는다")
    void 카카오_회원번호로_찾는다() {
        // given
        long userId = userRepository.save(User.ofKakao("40012345", "방장", null)).getUserId();

        // when — 재로그인 때 도는 조회
        User found = userRepository
                .findByProviderAndProviderId(Provider.KAKAO, "40012345")
                .orElseThrow();

        // then — 회원번호와 별개로 서비스가 발급한 식별자를 쓴다
        assertThat(found.getUserId()).isEqualTo(userId);
        assertThat(found.getProvider()).isEqualTo(Provider.KAKAO);
    }

    @Test
    @DisplayName("같은 카카오 계정은 두 행이 될 수 없다")
    void 같은_카카오_계정은_한_행이다() {
        // given
        userRepository.saveAndFlush(User.ofKakao("40012345", "방장", null));

        // when / then — 동시에 두 번 로그인해도 DB 가 막는다
        assertThatThrownBy(() -> userRepository.saveAndFlush(User.ofKakao("40012345", "다른이름", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
