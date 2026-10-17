package com.ninemensmorris.auth.domain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

// 인증 성공 후 식별자만 들고 다니는 최소 구현
// getAttributes / getAuthorities 는 null 을 돌려주면 OAuth2User 계약 위반이라 빈 값을 돌려줌
public record CustomOAuth2User(long userId) implements OAuth2User {

    @Override
    public Map<String, Object> getAttributes() {
        return Map.of();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
