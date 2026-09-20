package com.ninemensmorris.user.dto.response;

import com.ninemensmorris.core.rating.Tier;
import com.ninemensmorris.user.domain.User;

public record MyProfileResponse(
        long userId,
        String nickname,
        String imageUrl,
        int mmr,
        int peakMmr,
        Tier tier,
        int wins,
        int losses,
        int draws,
        Integer rank,
        // 화면에서 "게스트" 로 보여주고 로그인 유도를 띄우는 기준
        boolean visitor) {

    public static MyProfileResponse of(User user, Integer rank) {
        return new MyProfileResponse(
                user.getUserId(),
                user.getNickname(),
                user.getImageUrl(),
                user.getMmr(),
                user.getPeakMmr(),
                Tier.of(user.getMmr(), user.gamesPlayed()),
                user.getWins(),
                user.getLosses(),
                user.getDraws(),
                rank,
                user.isVisitor());
    }
}
