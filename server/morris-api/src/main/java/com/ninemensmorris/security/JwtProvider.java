package com.ninemensmorris.security;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtProvider {

    @Value("${JWT_SECRET_KEY}")
    private String secretKey;

    @Value("${ACCESS_TOKEN_EXPIRATION}")
    private Long accessTokenExpirationPeriod;

    @Value("${VISITOR_TOKEN_EXPIRATION}")
    private Long visitorTokenExpirationPeriod;

    private SecretKey signingKey;
    private JwtParser parser;

    @PostConstruct
    void init() {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(this.secretKey));
        this.parser = Jwts.parser().verifyWith(this.signingKey).build();
    }

    public String generateToken(Long userId, Long expirationPeriod) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationPeriod);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiration)
                .signWith(this.signingKey)
                .compact();
    }

    public String generateAccessToken(Long userId) {
        return generateToken(userId, accessTokenExpirationPeriod);
    }

    // 비로그인 사용자는 토큰을 잃으면 그 신원으로 돌아올 방법이 없다
    // 짧게 잡으면 게임 도중 만료되고, 탈취돼도 전적도 레이팅도 없어 잃을 것이 없으므로 길게 둔다
    public String generateVisitorToken(Long userId) {
        return generateToken(userId, visitorTokenExpirationPeriod);
    }

    public boolean validateToken(String token) {
        try {
            this.parser.parseSignedClaims(token);
            return true;
        } catch (Exception exception) {
            // 만료 토큰 접속은 정상 흐름. ERROR 로 남기면 로그 도배
            log.debug("JWT 검증 실패: {}", exception.getMessage());
            return false;
        }
    }

    public Long extractSubject(String token) {
        String userIdString = this.parser.parseSignedClaims(token).getPayload().getSubject();
        return Long.parseLong(userIdString);
    }
}
