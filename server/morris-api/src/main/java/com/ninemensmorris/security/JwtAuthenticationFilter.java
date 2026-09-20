package com.ninemensmorris.security;

import com.ninemensmorris.common.logging.LogContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// 토큰은 Authorization 헤더로만 받는다
// 쿠키는 프론트가 GitHub Pages 에 있어 cross-site 가 되므로 쓸 수 없음
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenAuthenticator tokenAuthenticator;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 인증에 실패해도 체인은 계속 진행. 접근 제어는 SecurityConfig 의 인가 규칙이 담당
        Authentication authentication = tokenAuthenticator.authenticate(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (authentication != null) {
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);
            // 요청자가 확정되는 유일한 지점. MDC 도 여기서 채운다
            // 비우는 것은 MdcRequestFilter 의 finally 가 한다
            AuthenticatedUser actor = AuthenticatedUser.from(authentication);
            if (actor != null) {
                LogContext.putUserId(actor.id());
            }
        }

        filterChain.doFilter(request, response);
    }
}
