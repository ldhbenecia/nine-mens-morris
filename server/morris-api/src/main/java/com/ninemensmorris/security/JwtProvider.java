package com.ninemensmorris.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtProvider {

    private static final String ROLE_CLAIM = "role";
    private static final String ROLE_USER = "ROLE_USER";
    private static final String ROLE_VISITOR = "ROLE_VISITOR";

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpirationPeriod;

    @Value("${jwt.visitor-token-expiration}")
    private Long visitorTokenExpirationPeriod;

    private SecretKey signingKey;
    private JwtParser parser;

    @PostConstruct
    void init() {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(this.secretKey));
        this.parser = Jwts.parser().verifyWith(this.signingKey).build();
    }

    // 역할을 토큰에 실어 요청마다 사용자를 조회하지 않게 함
    // 한 userId 의 역할은 바뀌지 않음. 회원/게스트는 가입 경로로 정해짐
    // role 이 null 이면 클레임을 넣지 않음. 클레임이 없던 시절 토큰을 흉내 내는 테스트용
    String generateToken(Long userId, String role, Long expirationPeriod) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationPeriod);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(ROLE_CLAIM, role)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(this.signingKey)
                .compact();
    }

    public String generateAccessToken(Long userId) {
        return generateToken(userId, ROLE_USER, accessTokenExpirationPeriod);
    }

    // 비로그인 사용자는 토큰을 잃으면 그 신원으로 돌아올 방법이 없다
    // 짧게 잡으면 게임 도중 만료되고, 탈취돼도 전적도 레이팅도 없어 잃을 것이 없으므로 길게 둔다
    public String generateVisitorToken(Long userId) {
        return generateToken(userId, ROLE_VISITOR, visitorTokenExpirationPeriod);
    }

    // 서명·만료 검증과 값 꺼내기를 한 번의 파싱으로. 실패하면 empty
    public Optional<TokenClaims> parse(String token) {
        try {
            Claims claims = this.parser.parseSignedClaims(token).getPayload();
            return Optional.of(
                    new TokenClaims(Long.parseLong(claims.getSubject()), claims.get(ROLE_CLAIM, String.class)));
        } catch (Exception exception) {
            // 만료 토큰 접속은 정상 흐름. ERROR 로 남기면 로그 도배
            log.debug("JWT 검증 실패: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    // role 은 클레임을 넣기 전에 발급된 토큰이면 null
    public record TokenClaims(long userId, String role) {}
}
