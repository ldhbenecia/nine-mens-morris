package com.ninemensmorris.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ninemensmorris.support.IntegrationTestSupport;
import com.ninemensmorris.user.domain.User;
import com.ninemensmorris.user.repository.UserRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

// 인증은 모든 요청이 지나는 경로. 여기서 DB 를 치면 모든 API 가 쿼리를 하나씩 더 냄
class AuthenticationQueryCountTest extends IntegrationTestSupport {

    // 없는 방. 인증을 통과한 뒤 메모리만 보고 404 로 끝나 이 요청의 쿼리는 전부 인증 몫
    private static final String NO_DB_PATH = "/api/v1/rooms/987654321";

    private final TestRestTemplate rest = new TestRestTemplate();

    @LocalServerPort
    private int port;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private TokenAuthenticator tokenAuthenticator;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
    }

    @AfterEach
    void tearDown() {
        statistics.setStatisticsEnabled(false);
    }

    @Test
    @DisplayName("토큰만으로 인증하고 사용자를 조회하지 않는다")
    void 인증에_쿼리가_없다() {
        // given
        User member = userRepository.save(User.ofKakao("8001", "회원", null));
        String token = jwtProvider.generateAccessToken(member.getUserId());
        statistics.clear();

        // when
        var response = rest.exchange(
                "http://localhost:" + port + NO_DB_PATH, HttpMethod.GET, new HttpEntity<>(bearer(token)), String.class);

        // then — 401 이 아니라 404 면 인증은 통과한 것
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(statistics.getPrepareStatementCount()).isZero();
    }

    @Test
    @DisplayName("게스트 토큰은 게스트 권한으로 인증된다")
    void 게스트_토큰은_게스트_권한이다() {
        // given
        String token = jwtProvider.generateVisitorToken(1L);

        // when
        var authentication = tokenAuthenticator.authenticate("Bearer " + token);

        // then
        assertThat(authentication.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_VISITOR");
    }

    @Test
    @DisplayName("역할 클레임이 없는 예전 토큰도 DB 를 보고 인증된다")
    void 예전_토큰도_인증된다() {
        // given — 배포 직후에는 클레임 없이 발급된 토큰이 아직 살아 있음
        User member = userRepository.save(User.ofKakao("8002", "예전회원", null));
        String legacy = jwtProvider.generateToken(member.getUserId(), null, 60_000L);

        // when
        var authentication = tokenAuthenticator.authenticate("Bearer " + legacy);

        // then
        assertThat(authentication.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_USER");
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return headers;
    }
}
