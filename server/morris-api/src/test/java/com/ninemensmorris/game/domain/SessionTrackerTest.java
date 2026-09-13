package com.ninemensmorris.game.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SessionTrackerTest {

    @Test
    @DisplayName("세션 하나가 끊겨도 다른 세션이 남아 있으면 접속 중이다")
    void 다른_세션이_남으면_접속_중이다() {
        // given — 같은 사용자가 두 탭을 열어 둔 상태
        SessionTracker tracker = new SessionTracker();
        tracker.add(1L, "session-a");
        tracker.add(1L, "session-b");

        // when
        tracker.remove("session-a");

        // then
        assertThat(tracker.hasActiveSession(1L)).isTrue();
    }

    @Test
    @DisplayName("마지막 세션이 끊기면 접속이 끊긴 것으로 본다")
    void 마지막_세션이_끊기면_접속이_끊긴다() {
        // given
        SessionTracker tracker = new SessionTracker();
        tracker.add(1L, "session-a");

        // when
        tracker.remove("session-a");

        // then
        assertThat(tracker.hasActiveSession(1L)).isFalse();
    }

    @Test
    @DisplayName("끊긴 세션의 주인을 돌려준다")
    void 끊긴_세션의_주인을_돌려준다() {
        // given
        SessionTracker tracker = new SessionTracker();
        tracker.add(7L, "session-a");

        // when, then
        assertThat(tracker.remove("session-a")).contains(7L);
    }

    @Test
    @DisplayName("추적하지 않던 세션은 주인이 없다")
    void 추적하지_않던_세션은_주인이_없다() {
        // given
        SessionTracker tracker = new SessionTracker();

        // when, then
        assertThat(tracker.remove("unknown")).isEmpty();
    }
}
