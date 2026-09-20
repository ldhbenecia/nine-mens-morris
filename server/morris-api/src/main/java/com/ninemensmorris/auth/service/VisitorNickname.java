package com.ninemensmorris.auth.service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

// 비로그인 사용자의 닉네임을 서버가 정한다
// 직접 짓게 하면 그 입력창이 다시 진입 장벽이 된다. 로그인을 없애는 의미가 사라짐
//
// nickname 에 유니크 제약이 없으므로 충돌해도 저장은 성공한다
// 뒤에 붙는 네 자리는 같은 이름이 화면에 겹쳐 보이는 것을 줄이려는 것일 뿐 유일성 보장이 아님
final class VisitorNickname {

    private static final List<String> ADJECTIVES =
            List.of("조용한", "재빠른", "신중한", "용감한", "느긋한", "성실한", "엉뚱한", "단단한", "날렵한", "차분한", "진지한", "상냥한");

    private static final List<String> NOUNS =
            List.of("물맷돌", "돌지기", "말판꾼", "초심자", "수읽기", "외통수", "길잡이", "맷돌", "세줄", "한수");

    private VisitorNickname() {}

    static String generate() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return ADJECTIVES.get(random.nextInt(ADJECTIVES.size()))
                + NOUNS.get(random.nextInt(NOUNS.size()))
                + random.nextInt(1000, 10000);
    }
}
