package com.ninemensmorris.observability;

import com.ninemensmorris.common.logging.LogContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// HTTP 요청에 traceId 를 붙인다. userId 는 인증이 끝나는 JwtAuthenticationFilter 에서 이어 넣는다
//
// 보안 필터 체인(-100)보다 앞이어야 한다. 뒤면 401 을 만드는 시점에 MDC 가 비어 있다
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcRequestFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        LogContext.startTrace();
        try {
            filterChain.doFilter(request, response);
        } finally {
            LogContext.clear();
        }
    }

    // 프로브는 10초마다 들어오는데 묶을 로그가 없다
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }
}
