package com.ninemensmorris.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.ninemensmorris.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

// 재접속 유예가 끝났을 때 "아직 그 방에 붙어 있는가" 를 서버 전체 기준으로 보는지
// 서버 둘은 같은 Redis 를 보는 인스턴스 둘로 흉내 냄
class ClusterPresenceTest extends IntegrationTestSupport {

    private static final long USER = 1L;
    private static final long ROOM = 7L;
    private static final long OTHER_ROOM = 8L;

    @Autowired
    private StringRedisTemplate redis;

    private ClusterPresence serverA;
    private ClusterPresence serverB;

    @BeforeEach
    void setUp() {
        redis.delete(redis.keys("presence:*"));
        serverA = new ClusterPresence(redis, "server-a");
        serverB = new ClusterPresence(redis, "server-b");
        // 운영에서는 스케줄러가 기동 직후부터 보냄
        serverA.heartbeat();
        serverB.heartbeat();
    }

    @Test
    @DisplayName("한 서버에서 끊겨도 다른 서버로 다시 붙었으면 아직 보고 있는 것이다")
    void 다른_서버로_다시_붙으면_보고_있다() {
        // given — 서버 A 가 내려가며 끊기고 서버 B 로 다시 붙음
        serverA.enter(USER, ROOM, "s1");
        serverB.enter(USER, ROOM, "s2");

        // when
        serverA.leave(USER, ROOM, "s1");

        // then — A 가 자기 세션만 보면 나간 것으로 판단해 기권 처리함
        assertThat(serverA.isWatching(USER, ROOM)).isTrue();
    }

    @Test
    @DisplayName("다른 방을 보는 세션은 그 방을 보고 있다고 치지 않는다")
    void 다른_방의_세션은_무관하다() {
        // given — 로비를 다른 탭에 띄워 둔 채 게임 탭만 닫음
        serverA.enter(USER, OTHER_ROOM, "lobby");
        serverA.enter(USER, ROOM, "game");

        // when
        serverA.leave(USER, ROOM, "game");

        // then
        assertThat(serverB.isWatching(USER, ROOM)).isFalse();
        assertThat(serverB.isWatching(USER, OTHER_ROOM)).isTrue();
    }

    @Test
    @DisplayName("죽은 서버에 남은 세션은 붙어 있는 것으로 보지 않는다")
    void 죽은_서버의_세션은_없는_것이다() {
        // given — 서버 A 가 해제 이벤트도 못 보내고 죽음. 집합에 세션이 남음
        serverA.enter(USER, ROOM, "s1");

        // when — 생존 키가 사라짐 (TTL 만료와 같음)
        serverA.retire();

        // then
        assertThat(serverB.isWatching(USER, ROOM)).isFalse();
    }

    @Test
    @DisplayName("살아 있는 서버의 세션이 하나라도 있으면 보고 있다")
    void 살아_있는_세션이_있으면_보고_있다() {
        // given
        serverA.enter(USER, ROOM, "s1");
        serverB.enter(USER, ROOM, "s2");

        // when
        serverA.retire();

        // then
        assertThat(serverA.isWatching(USER, ROOM)).isTrue();
    }
}
