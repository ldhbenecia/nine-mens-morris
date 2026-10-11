package com.ninemensmorris.game.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// "아직 방에 붙어 있는가" 판단은 ClusterPresenceTest 로 옮김. 여기는 이 서버의 세션 표만
class SessionTrackerTest {

    private static final long USER = 1L;
    private static final long ROOM = 7L;

    @Test
    @DisplayName("방을 구독하지 않은 세션이 끊기면 정리할 방이 없다")
    void 방을_안_보던_세션은_방이_없다() {
        // given
        SessionTracker tracker = new SessionTracker();
        tracker.add(USER, "a");

        // when
        SessionTracker.Departure departure = tracker.remove("a").orElseThrow();

        // then
        assertThat(departure.userId()).isEqualTo(USER);
        assertThat(departure.roomId()).isNull();
    }

    @Test
    @DisplayName("끊긴 세션의 주인과 보던 방을 돌려준다")
    void 주인과_방을_돌려준다() {
        // given
        SessionTracker tracker = new SessionTracker();
        tracker.add(USER, "a");
        tracker.enterRoom("a", ROOM);

        // when
        SessionTracker.Departure departure = tracker.remove("a").orElseThrow();

        // then
        assertThat(departure.userId()).isEqualTo(USER);
        assertThat(departure.roomId()).isEqualTo(ROOM);
    }

    @Test
    @DisplayName("추적하지 않던 세션은 주인이 없다")
    void 추적하지_않던_세션은_주인이_없다() {
        // given
        SessionTracker tracker = new SessionTracker();

        // when, then
        assertThat(tracker.remove("unknown")).isEmpty();
    }

    @Test
    @DisplayName("연결되지 않은 세션에는 방을 붙이지 않는다")
    void 모르는_세션에는_방을_붙이지_않는다() {
        // given — CONNECT 를 거치지 않은 세션이 방에 들어온 것처럼 보이면 안 됨
        SessionTracker tracker = new SessionTracker();

        // when
        tracker.enterRoom("ghost", ROOM);

        // then
        assertThat(tracker.remove("ghost")).isEmpty();
        assertThat(tracker.openSessions()).isZero();
    }
}
