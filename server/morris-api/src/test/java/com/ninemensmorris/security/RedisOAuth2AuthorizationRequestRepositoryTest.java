package com.ninemensmorris.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ninemensmorris.support.IntegrationTestSupport;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

// 로그인 시작과 콜백을 서로 다른 서버가 받아도 인가 요청을 찾는지
// 서버 둘은 같은 Redis 를 보는 저장소 인스턴스 둘로 흉내 냄
class RedisOAuth2AuthorizationRequestRepositoryTest extends IntegrationTestSupport {

    private static final String STATE = "state-123";

    @Autowired
    private StringRedisTemplate redis;

    private RedisOAuth2AuthorizationRequestRepository serverA;
    private RedisOAuth2AuthorizationRequestRepository serverB;

    @BeforeEach
    void setUp() {
        redis.delete(redis.keys("oauth2:authorization-request:*"));
        serverA = new RedisOAuth2AuthorizationRequestRepository(redis);
        serverB = new RedisOAuth2AuthorizationRequestRepository(redis);
    }

    private static OAuth2AuthorizationRequest 인가_요청() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .clientId("client-id")
                .redirectUri("https://api.example.com/api/oauth2/kakao")
                .state(STATE)
                .attributes(Map.of(OAuth2ParameterNames.REGISTRATION_ID, "kakao"))
                .build();
    }

    private static MockHttpServletRequest 콜백(String state) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/oauth2/kakao");
        request.setParameter(OAuth2ParameterNames.STATE, state);
        return request;
    }

    @Test
    @DisplayName("한 서버가 저장한 인가 요청을 다른 서버가 콜백에서 찾는다")
    void 다른_서버가_찾는다() {
        // given
        serverA.saveAuthorizationRequest(인가_요청(), new MockHttpServletRequest(), new MockHttpServletResponse());

        // when
        OAuth2AuthorizationRequest loaded = serverB.loadAuthorizationRequest(콜백(STATE));

        // then — 세션에 두면 여기서 null 이 되어 authorization_request_not_found 로 로그인이 실패함
        assertThat(loaded).isNotNull();
        assertThat(loaded.getState()).isEqualTo(STATE);
        assertThat(loaded.getClientId()).isEqualTo("client-id");
        assertThat(loaded.getRedirectUri()).isEqualTo("https://api.example.com/api/oauth2/kakao");
        assertThat(loaded.<String>getAttribute(OAuth2ParameterNames.REGISTRATION_ID))
                .isEqualTo("kakao");
    }

    @Test
    @DisplayName("콜백에서 꺼낸 인가 요청은 다시 쓸 수 없다")
    void 한_번만_쓴다() {
        // given
        serverA.saveAuthorizationRequest(인가_요청(), new MockHttpServletRequest(), new MockHttpServletResponse());

        // when
        OAuth2AuthorizationRequest removed =
                serverB.removeAuthorizationRequest(콜백(STATE), new MockHttpServletResponse());

        // then — 남아 있으면 같은 state 로 콜백을 다시 보내 재사용할 수 있음
        assertThat(removed).isNotNull();
        assertThat(serverA.loadAuthorizationRequest(콜백(STATE))).isNull();
    }

    @Test
    @DisplayName("저장한 적 없는 state 나 state 없는 요청은 찾지 못한다")
    void 모르는_state_는_없다() {
        // given
        serverA.saveAuthorizationRequest(인가_요청(), new MockHttpServletRequest(), new MockHttpServletResponse());

        // then
        assertThat(serverB.loadAuthorizationRequest(콜백("forged"))).isNull();
        assertThat(serverB.loadAuthorizationRequest(new MockHttpServletRequest()))
                .isNull();
    }

    @Test
    @DisplayName("로그인을 마치지 않고 떠난 요청은 만료로 사라진다")
    void 만료가_걸린다() {
        // when
        serverA.saveAuthorizationRequest(인가_요청(), new MockHttpServletRequest(), new MockHttpServletResponse());

        // then
        Long ttl = redis.getExpire("oauth2:authorization-request:" + STATE);
        assertThat(ttl).isPositive().isLessThanOrEqualTo(300);
    }
}
