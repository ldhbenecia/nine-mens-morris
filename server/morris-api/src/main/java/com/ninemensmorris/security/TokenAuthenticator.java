package com.ninemensmorris.security;

import com.ninemensmorris.user.domain.User;
import com.ninemensmorris.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

// 토큰 하나를 인증 주체로 바꾸는 유일한 지점
// HTTP 요청과 STOMP CONNECT 가 같은 규칙을 써야 해서 한 곳에 둠
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenAuthenticator {

    private static final String BEARER = "Bearer ";

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    // 실패하면 null. 접근 제어는 인가 규칙이 하므로 여기서 예외를 던지지 않음
    public Authentication authenticate(String bearerHeader) {
        String token = stripBearer(bearerHeader);
        // 검증 실패 사유는 JwtProvider 가 debug 로 남김. 여기서 또 남기면 요청당 2줄
        if (token == null || !jwtProvider.validateToken(token)) {
            return null;
        }

        try {
            Long userId = jwtProvider.extractSubject(token);
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                // 서명은 유효한데 사용자가 없음. 탈퇴했거나 DB 가 초기화된 경우
                log.warn("유효한 토큰이지만 사용자를 찾을 수 없음 userId={}", userId);
                return null;
            }
            return new UsernamePasswordAuthenticationToken(
                    userId, null, List.of(new SimpleGrantedAuthority(user.getRole())));
        } catch (RuntimeException exception) {
            log.warn("JWT 인증 처리 실패: {}", exception.toString());
            return null;
        }
    }

    private String stripBearer(String header) {
        if (header == null || !header.startsWith(BEARER)) {
            return null;
        }
        String token = header.substring(BEARER.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
