package com.ninemensmorris.config;

import com.ninemensmorris.auth.service.CustomOAuth2UserService;
import com.ninemensmorris.security.JwtAuthenticationFilter;
import com.ninemensmorris.security.OAuth2SuccessHandler;
import com.ninemensmorris.security.UnauthorizedEntryPoint;
import jakarta.servlet.DispatcherType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService oAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final UnauthorizedEntryPoint unauthorizedEntryPoint;

    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .oauth2Login(oauth2 -> oauth2.redirectionEndpoint(endpoint -> endpoint.baseUri("/api/oauth2/kakao"))
                        .userInfoEndpoint(endpoint -> endpoint.userService(oAuth2UserService))
                        .successHandler(oAuth2SuccessHandler))
                // 토큰이 클라이언트에 있으므로 서버가 지울 것이 없다
                // 무효화 목록을 두기 전까지 로그아웃은 클라이언트가 토큰을 버리는 것으로 끝남
                .logout(logout -> logout.logoutUrl("/api/v1/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 기본을 거부로 둔다. permitAll 이 기본이면 실수로 열린 엔드포인트를 못 잡는다
                //
                // ERROR 디스패치를 먼저 열어 둔다. 이게 없으면 검증 실패·깨진 JSON 처럼
                // 컨트롤러 밖에서 터진 400 이 /error 로 포워딩되면서 기본 거부에 걸려
                // 401 "로그인이 필요합니다" 로 바뀐다
                .authorizeHttpRequests(auth -> auth.dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()
                        // 프로브와 스크레이프는 인증을 붙일 수 없다
                        // 쿠버네티스 kubelet 도 Prometheus 도 토큰을 들고 오지 않음
                        // 대신 인그레스에서 /actuator 를 라우팅하지 않아 외부에서는 닿지 않는다
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/prometheus")
                        .permitAll()
                        .requestMatchers("/oauth2/**", "/api/oauth2/**")
                        .permitAll()
                        .requestMatchers("/ws/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/rankings")
                        .permitAll()
                        // 비로그인 계정 발급은 신원이 없는 상태에서 부르는 것이라 열어 둔다
                        // 대신 IP 당 한도를 둔다
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/visitors")
                        .permitAll()
                        // 로그인 여부가 아니라 권한으로 본다
                        // 나중에 정지 계정 같은 역할이 생겨도 명시적으로 열어야 들어온다
                        .requestMatchers("/api/v1/**")
                        .hasAnyRole("USER", "VISITOR")
                        .anyRequest()
                        .denyAll())
                .exceptionHandling(handling -> handling.authenticationEntryPoint(unauthorizedEntryPoint))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 하드코딩하지 않는다. GitHub Pages 주소는 배포 환경마다 다르다
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
