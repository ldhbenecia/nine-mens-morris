package com.ninemensmorris.game.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SessionTrackerTest {

    private static final long USER = 1L;
    private static final long ROOM = 7L;
    private static final long OTHER_ROOM = 8L;

    @Test
    @DisplayName("같은 방에 붙은 다른 세션이 있으면 아직 보고 있는 것이다")
    void 같은_방의_다른_세션이_있으면_보고_있다() {
        // given — 새로고침이나 화면 이동으로 같은 방에 두 세션이 겹치는 순간
        SessionTracker tracker = new SessionTracker();
        tracker.add(USER, "a");
        tracker.add(USER, "b");
        tracker.enterRoom("a", ROOM);
        tracker.enterRoom("b", ROOM);

        // when
        tracker.remove("a");

        // then
        assertThat(tracker.isWatching(USER, ROOM)).isTrue();
    }

    @Test
    @DisplayName("다른 방을 보는 세션은 그 방을 보고 있다고 치지 않는다")
    void 다른_방의_세션은_무관하다() {
        // given — 로비를 다른 탭에 띄워 둔 채 게임 탭만 닫으면
        //         예전에는 접속 중으로 보고 정산을 건너뛰었다
        SessionTracker tracker = new SessionTracker();
        tracker.add(USER, "lobby");
        tracker.add(USER, "game");
        tracker.enterRoom("lobby", OTHER_ROOM);
        tracker.enterRoom("game", ROOM);

        // when
        tracker.remove("game");

        // then
        assertThat(tracker.isWatching(USER, ROOM)).isFalse();
        assertThat(tracker.isWatching(USER, OTHER_ROOM)).isTrue();
    }

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
        assertThat(tracker.isWatching(USER, ROOM)).isFalse();
        assertThat(tracker.remove("ghost")).isEmpty();
    }

    @Test
    @DisplayName("접속과 해제가 겹쳐도 살아 있는 세션이 사라지지 않는다")
    void 접속과_해제가_겹쳐도_세션이_유실되지_않는다() throws InterruptedException {
        // given — compute 밖에서 집합을 고치면 마지막 세션 해제가
        //         방금 추가된 세션까지 같이 날려 버린다
        for (int attempt = 0; attempt < 500; attempt++) {
            SessionTracker tracker = new SessionTracker();
            tracker.add(USER, "old");
            tracker.enterRoom("old", ROOM);

            // when — 새 세션 추가와 기존 세션 해제를 동시에
            Thread adding = new Thread(() -> {
                tracker.add(USER, "new");
                tracker.enterRoom("new", ROOM);
            });
            Thread removing = new Thread(() -> tracker.remove("old"));
            adding.start();
            removing.start();
            adding.join();
            removing.join();

            // then
            assertThat(tracker.isWatching(USER, ROOM)).isTrue();
        }
    }
}
