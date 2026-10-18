package com.ninemensmorris.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;

// 카카오로 보낸 인가 요청을 콜백이 올 때까지 들고 있는 곳
//
// 기본 구현은 서버 메모리의 HTTP 세션에 둠. 서버가 여러 대면 로그인 시작을 받은 서버와
// 콜백을 받은 서버가 달라 authorization_request_not_found 로 로그인이 확률적으로 실패함
// state 를 키로 Redis 에 두어 어느 서버가 콜백을 받아도 찾음
//
// JSON 은 Spring Security 의 허용 목록 모듈로만 읽음. 아무 타입이나 되살리는 직렬화는 쓰지 않음
// 앱의 ObjectMapper 와 따로 둠. 이 모듈은 타입 정보를 켜므로 다른 응답의 모양까지 바꿈
@Component
public class RedisOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String KEY_PREFIX = "oauth2:authorization-request:";
    private static final Duration TTL = Duration.ofMinutes(5); // 카카오 로그인 화면에 머물 수 있는 시간

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RedisOAuth2AuthorizationRequestRepository(StringRedisTemplate redis) {
        this.redis = redis;
        objectMapper.registerModules(
                SecurityJackson2Modules.getModules(getClass().getClassLoader()));
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String state = request.getParameter(OAuth2ParameterNames.STATE);
        return state == null ? null : read(redis.opsForValue().get(key(state)));
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequest(request, response);
            return;
        }
        redis.opsForValue().set(key(authorizationRequest.getState()), write(authorizationRequest), TTL);
    }

    // 꺼내면서 지움. 같은 state 로 콜백을 다시 보내 한 번 쓴 요청을 재사용하지 못하게
    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request, HttpServletResponse response) {
        String state = request.getParameter(OAuth2ParameterNames.STATE);
        return state == null ? null : read(redis.opsForValue().getAndDelete(key(state)));
    }

    private static String key(String state) {
        return KEY_PREFIX + state;
    }

    private String write(OAuth2AuthorizationRequest authorizationRequest) {
        try {
            return objectMapper.writeValueAsString(authorizationRequest);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("인가 요청 직렬화 실패", exception);
        }
    }

    private OAuth2AuthorizationRequest read(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, OAuth2AuthorizationRequest.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("인가 요청 역직렬화 실패", exception);
        }
    }
}
