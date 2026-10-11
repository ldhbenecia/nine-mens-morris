package com.ninemensmorris.user.dto.response;

import com.ninemensmorris.core.rating.Tier;
import com.ninemensmorris.user.domain.User;

// 대전 상대에게 보이는 정보. 랭킹에 이미 공개된 것만 담음
public record UserProfileResponse(
        long userId,
        String nickname,
        String imageUrl,
        int mmr,
        Tier tier,
        int wins,
        int losses,
        int draws,
        boolean visitor) { // 게스트는 MMR 이 움직이지 않아 화면이 티어를 숨기는 기준

    public static UserProfileResponse of(User user) {
        return new UserProfileResponse(
                user.getUserId(),
                user.getNickname(),
                user.getImageUrl(),
                user.getMmr(),
                Tier.of(user.getMmr(), user.gamesPlayed()),
                user.getWins(),
                user.getLosses(),
                user.getDraws(),
                user.isVisitor());
    }
}
