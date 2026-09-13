package com.ninemensmorris.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ninemensmorris.common.exception.ErrorResponse;
import com.ninemensmorris.common.response.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

// 인증되지 않은 요청. 기존에는 403 을 반환해 프론트의 재로그인 분기가 꼬였음
// 403 은 "인증은 됐지만 권한이 없음" 이고 여기는 "인증이 안 됨" 이라 401 이 맞음
//
// 본문을 손으로 조립하면 다른 실패 응답과 모양이 달라지므로 ErrorResponse 를 그대로 직렬화함
@Component
@RequiredArgsConstructor
public class UnauthorizedEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(ErrorCode.UNAUTHORIZED));
    }
}
