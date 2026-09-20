package com.ninemensmorris.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.ninemensmorris.match.repository.MatchRepository;
import com.ninemensmorris.support.IntegrationTestSupport;
import com.ninemensmorris.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

// 로그인 없이 신원을 받아 게임까지 갈 수 있어야 한다
// 인가 규칙을 denyAll 기본으로 두고 있어 실제로 요청을 보내 봐야 열렸는지 알 수 있다
class VisitorAuthTest extends IntegrationTestSupport {

    private final TestRestTemplate rest = new TestRestTemplate();

    @LocalServerPort
    private int port;

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
    @DisplayName("로그인 없이 신원을 받고 그 토큰으로 방을 만들 수 있다")
    void 로그인_없이_방을_만든다() {
        // given / when — 요청 본문도 인증도 없다
        var issued = rest.exchange(url("/api/v1/auth/visitors"), HttpMethod.POST, null, VisitorToken.class);

        // then
        assertThat(issued.getStatusCode().value()).isEqualTo(201);
        String token = issued.getBody().accessToken();
        assertThat(token).isNotBlank();

        // 발급받은 토큰이 실제로 인가를 통과하는지
        var created = rest.exchange(
                url("/api/v1/rooms"),
                HttpMethod.POST,
                new HttpEntity<>(new CreateRoomPayload("게스트 방"), bearer(token)),
                CreatedRoom.class);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
    }

    @Test
    @DisplayName("비로그인 계정의 프로필은 게스트로 표시되고 등수가 없다")
    void 비로그인_계정은_등수가_없다() {
        // given
        String token = rest.exchange(url("/api/v1/auth/visitors"), HttpMethod.POST, null, VisitorToken.class)
                .getBody()
                .accessToken();

        // when
        var profile =
                rest.exchange(url("/api/v1/users/me"), HttpMethod.GET, new HttpEntity<>(bearer(token)), Profile.class);

        // then
        assertThat(profile.getStatusCode().value()).isEqualTo(200);
        assertThat(profile.getBody().visitor()).isTrue();
        assertThat(profile.getBody().rank()).isNull();
        assertThat(profile.getBody().nickname()).isNotBlank();
    }

    @Test
    @DisplayName("토큰이 없으면 401 이다")
    void 토큰이_없으면_거절한다() {
        // when — 게스트를 열었다고 해서 익명 접근이 열린 것은 아니다
        var profile = rest.exchange(url("/api/v1/users/me"), HttpMethod.GET, null, String.class);

        // then
        assertThat(profile.getStatusCode().value()).isEqualTo(401);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return headers;
    }

    private record VisitorToken(String accessToken) {}

    private record Profile(String nickname, Integer rank, boolean visitor) {}

    private record CreateRoomPayload(String title) {}

    private record CreatedRoom(long roomId, String title) {}
}
