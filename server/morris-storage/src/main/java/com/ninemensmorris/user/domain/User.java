package com.ninemensmorris.user.domain;

import com.ninemensmorris.core.rating.Rating;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 식별자는 서비스가 발급하고 인증 수단은 속성으로 둔다
// 카카오 회원번호를 그대로 기본키로 쓰던 때는 회원번호가 없는 비로그인 사용자를 만들 수 없었음
//
// (provider, provider_id) 유니크는 같은 카카오 계정이 두 행이 되는 것을 막음
// 비로그인 사용자는 provider_id 가 null 인데 MySQL 은 유니크 인덱스에서 null 을
// 서로 다른 값으로 보므로 그 행은 몇 개가 생기든 이 제약에 걸리지 않음
//
// 랭킹은 mmr 로 정렬하고 내 등수는 mmr 비교로 세므로 인덱스가 없으면 매번 풀스캔이다
// /rankings 는 로그인 없이도 열리는 엔드포인트라 더 그렇다
@Entity
@Table(
        name = "users",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_users_provider",
                        columnNames = {"provider", "provider_id"}),
        indexes = @Index(name = "idx_users_mmr", columnList = "mmr"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    private static final String ROLE_USER = "ROLE_USER";
    private static final String ROLE_VISITOR = "ROLE_VISITOR";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Provider provider;

    // 카카오 회원번호. 비로그인 사용자는 null
    @Column(length = 64)
    private String providerId;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(length = 500)
    private String imageUrl;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(nullable = false)
    private int mmr;

    @Column(nullable = false)
    private int peakMmr;

    @Column(nullable = false)
    private int wins;

    @Column(nullable = false)
    private int losses;

    @Column(nullable = false)
    private int draws;

    // 유휴 비로그인 계정 정리 기준
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private User(Provider provider, String providerId, String nickname, String imageUrl, String role) {
        this.provider = provider;
        this.providerId = providerId;
        this.nickname = nickname;
        this.imageUrl = imageUrl;
        this.role = role;
        this.mmr = Rating.INITIAL;
        this.peakMmr = Rating.INITIAL;
        this.createdAt = Instant.now();
    }

    public static User ofKakao(String providerId, String nickname, String imageUrl) {
        return new User(Provider.KAKAO, providerId, nickname, imageUrl, ROLE_USER);
    }

    // 로그인하지 않은 사용자. 닉네임은 서버가 정하고 프로필 이미지는 없다
    public static User visitor(String nickname) {
        return new User(Provider.VISITOR, null, nickname, null, ROLE_VISITOR);
    }

    public boolean isVisitor() {
        return provider == Provider.VISITOR;
    }

    public int gamesPlayed() {
        return wins + losses + draws;
    }

    // 세터 대신 의도가 드러나는 메서드로만 바꾼다
    // 하한과 최고 기록 갱신을 엔티티가 보장해야 호출부마다 빠뜨리지 않는다
    public void applyMatchResult(int mmrDelta, MatchOutcome outcome) {
        this.mmr = Rating.applyFloor(this.mmr + mmrDelta);
        this.peakMmr = Math.max(this.peakMmr, this.mmr);
        switch (outcome) {
            case WIN -> this.wins++;
            case LOSS -> this.losses++;
            case DRAW -> this.draws++;
        }
    }

    public void changeProfile(String nickname, String imageUrl) {
        this.nickname = nickname;
        this.imageUrl = imageUrl;
    }

    public enum MatchOutcome {
        WIN,
        LOSS,
        DRAW
    }
}
