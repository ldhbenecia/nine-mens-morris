package com.ninemensmorris.security;

import com.ninemensmorris.auth.domain.CustomOAuth2User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

// 로그인에 성공하면 토큰을 프래그먼트에 실어 프론트로 보낸다
//
// 쿠키를 쓰지 않는 이유는 프론트가 GitHub Pages 라 서버와 cross-site 이기 때문
// 쿼리스트링이 아니라 프래그먼트인 이유는 그쪽이 서버로 전송되지 않아
// 액세스 로그나 Referer 에 토큰이 남지 않기 때문
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final String CALLBACK_PATH = "/auth/callback";

    private final JwtProvider jwtProvider;

    @Value("${app.frontend-url}")
    private String domainUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        String accessToken = jwtProvider.generateAccessToken(Long.parseLong(oAuth2User.getName()));

        String encoded = URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
        getRedirectStrategy().sendRedirect(request, response, domainUrl + CALLBACK_PATH + "#token=" + encoded);
    }
}
