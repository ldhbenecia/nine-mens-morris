package com.ninemensmorris.game;

import static com.ninemensmorris.support.StompTestClient.next;
import static com.ninemensmorris.support.StompTestClient.subscribe;
import static org.assertj.core.api.Assertions.assertThat;

import com.ninemensmorris.NineMensMorrisApplication;
import com.ninemensmorris.game.dto.response.RoomEvent;
import com.ninemensmorris.game.dto.response.RoomEventType;
import com.ninemensmorris.security.JwtProvider;
import com.ninemensmorris.support.IntegrationTestSupport;
import com.ninemensmorris.support.StompTestClient;
import com.ninemensmorris.user.domain.User;
import com.ninemensmorris.user.repository.UserRepository;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.messaging.simp.stomp.StompSession;

// 서버 두 대에서 한 판
//
// 서버 A 는 이 테스트의 스프링 컨텍스트, 서버 B 는 같은 앱을 하나 더 띄운 것
// 둘은 같은 MySQL·Redis 를 보고 포트만 다름. 두 플레이어를 서로 다른 서버에 붙임
class MultiServerGameTest extends IntegrationTestSupport {

    // 이 시간이 지나도 다시 붙지 않으면 기권 처리됨 (WebSocketEventListener)
    private static final Duration RECONNECT_GRACE = Duration.ofSeconds(10);

    @LocalServerPort
    private int portA;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private UserRepository userRepository;

    private ConfigurableApplicationContext serverB;
    private int portB;
    private StompTestClient stomp;
    private User host;
    private User guest;

    @BeforeEach
    void setUp() {
        host = userRepository.save(User.ofKakao("multi-" + System.nanoTime(), "방장", null));
        guest = userRepository.save(User.ofKakao("multi-" + System.nanoTime(), "참가자", null));
        serverB = startServer("server-b");
        portB = serverB.getEnvironment().getRequiredProperty("local.server.port", Integer.class);
        stomp = new StompTestClient();
    }

    @AfterEach
    void tearDown() {
        stomp.close();
        if (serverB.isActive()) {
            serverB.close();
        }
    }

    // 명령줄 인자는 환경 변수보다 우선함. HOSTNAME 이 이미 있는 환경(CI)에서도 서버 id 가 겹치지 않게
    private static ConfigurableApplicationContext startServer(String name) {
        return new SpringApplicationBuilder(NineMensMorrisApplication.class)
                .run(
                        "--server.port=0",
                        "--HOSTNAME=" + name,
                        "--spring.datasource.url=" + MYSQL.getJdbcUrl(),
                        "--spring.datasource.username=" + MYSQL.getUsername(),
                        "--spring.datasource.password=" + MYSQL.getPassword(),
                        "--spring.data.redis.host=" + REDIS.getHost(),
                        "--spring.data.redis.port=" + REDIS.getMappedPort(6379));
    }

    @Test
    @DisplayName("서로 다른 서버에 붙은 두 사람이 한 판을 끝까지 둔다")
    void 다른_서버의_두_사람이_한_판을_둔다() throws Exception {
        // given — 방은 서버 A 에서 만들고 입장은 서버 B 로
        long roomId = createRoom(portA);
        join(portB, roomId);

        StompSession hostSession = stomp.connect(portA, bearer(host));
        StompSession guestSession = stomp.connect(portB, bearer(guest));
        BlockingQueue<RoomEvent> hostInbox = subscribe(hostSession, topic(roomId), RoomEvent.class);
        BlockingQueue<RoomEvent> guestInbox = subscribe(guestSession, topic(roomId), RoomEvent.class);
        Thread.sleep(500); // 서버마다 방 채널 구독이 잡히는 시간

        // when — 서버 A 에서 시작
        hostSession.send("/app/rooms/" + roomId + "/start", null);

        // then — 서버 B 에 붙은 참가자도 받음
        RoomEvent started = next(hostInbox);
        assertThat(started.type()).isEqualTo(RoomEventType.STARTED);
        assertThat(next(guestInbox).type()).isEqualTo(RoomEventType.STARTED);

        // when — 선공이 자기 서버에서, 후공이 자기 서버에서 한 수씩
        boolean hostFirst = started.state().currentTurnId() == host.getUserId();
        StompSession first = hostFirst ? hostSession : guestSession;
        StompSession second = hostFirst ? guestSession : hostSession;
        first.send("/app/rooms/" + roomId + "/place", new Place(0));
        RoomEvent afterFirst = next(hostInbox);
        next(guestInbox);
        second.send("/app/rooms/" + roomId + "/place", new Place(9));
        RoomEvent afterSecond = next(guestInbox);
        next(hostInbox);

        // then — 양쪽 수가 한 판에 모이고 revision 이 오름
        assertThat(afterSecond.state().board()[0]).isNotEqualTo("EMPTY");
        assertThat(afterSecond.state().board()[9]).isNotEqualTo("EMPTY");
        assertThat(afterSecond.state().revision())
                .isGreaterThan(afterFirst.state().revision());

        // when — 서버 B 쪽 사람이 기권
        guestSession.send("/app/rooms/" + roomId + "/resign", null);

        // then — 서버 A 쪽 방장에게 승리로 도착
        RoomEvent finished = next(hostInbox);
        assertThat(finished.type()).isEqualTo(RoomEventType.FINISHED);
        assertThat(finished.state().winnerId()).isEqualTo(host.getUserId());
    }

    @Test
    @DisplayName("판 도중 서버 하나가 내려가도 다른 서버로 다시 붙어 이어 둔다")
    void 서버가_내려가도_판이_이어진다() throws Exception {
        // given — 방장은 A, 참가자는 B. 한 수씩 둔 상태
        long roomId = createRoom(portA);
        join(portA, roomId);
        StompSession hostSession = stomp.connect(portA, bearer(host));
        StompSession guestOnB = stomp.connect(portB, bearer(guest));
        BlockingQueue<RoomEvent> hostInbox = subscribe(hostSession, topic(roomId), RoomEvent.class);
        BlockingQueue<RoomEvent> guestInbox = subscribe(guestOnB, topic(roomId), RoomEvent.class);
        Thread.sleep(500);

        hostSession.send("/app/rooms/" + roomId + "/start", null);
        RoomEvent started = next(hostInbox);
        next(guestInbox);
        boolean hostFirst = started.state().currentTurnId() == host.getUserId();
        (hostFirst ? hostSession : guestOnB).send("/app/rooms/" + roomId + "/place", new Place(0));
        RoomEvent beforeShutdown = next(hostInbox);

        // when — 서버 B 를 내림 (배포처럼 정상 종료). 내려가는 동안 참가자는 서버 A 로 다시 붙음
        CompletableFuture<Void> shutdown = CompletableFuture.runAsync(serverB::close);
        waitUntilDisconnected(guestOnB);
        StompSession guestOnA = stomp.connect(portA, bearer(guest));
        BlockingQueue<RoomEvent> guestInboxOnA = subscribe(guestOnA, topic(roomId), RoomEvent.class);
        BlockingQueue<RoomEvent> synced = subscribe(guestOnA, "/user/queue/sync", RoomEvent.class);
        Thread.sleep(500);
        guestOnA.send("/app/rooms/" + roomId + "/sync", null);

        // then — 판이 그대로 남아 있음. 서버 B 가 들고 있던 것이 아니라 Redis 에 있으므로
        RoomEvent snapshot = next(synced);
        assertThat(snapshot.type()).isEqualTo(RoomEventType.SNAPSHOT);
        assertThat(snapshot.state().board()).isEqualTo(beforeShutdown.state().board());
        assertThat(snapshot.state().status().name()).isEqualTo("PLAYING");

        // when — 이어서 둠. 이제 둘 다 서버 A
        StompSession nextMover = hostFirst ? guestOnA : hostSession;
        nextMover.send("/app/rooms/" + roomId + "/place", new Place(9));

        // then
        RoomEvent continued = next(hostInbox);
        assertThat(continued.type()).isEqualTo(RoomEventType.STATE_CHANGED);
        assertThat(continued.state().board()[9]).isNotEqualTo("EMPTY");
        assertThat(next(guestInboxOnA).state().board()[9]).isNotEqualTo("EMPTY");

        // then — 재접속 유예가 지나도 기권 처리되지 않음. 다른 서버로 다시 붙은 것을 서버 전체 기준으로 봄
        shutdown.get(30, TimeUnit.SECONDS);
        Thread.sleep(RECONNECT_GRACE.plusSeconds(2).toMillis());
        assertThat(hostInbox.poll(1, TimeUnit.SECONDS))
                .as("다른 서버로 다시 붙었는데 이탈로 처리됨")
                .isNull();
        BlockingQueue<RoomEvent> finalSync = subscribe(hostSession, "/user/queue/sync", RoomEvent.class);
        Thread.sleep(300);
        hostSession.send("/app/rooms/" + roomId + "/sync", null);
        assertThat(next(finalSync).state().status().name()).isEqualTo("PLAYING");
    }

    private static void waitUntilDisconnected(StompSession session) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (session.isConnected()) {
            assertThat(System.nanoTime()).as("서버가 내려가는데 연결이 끊기지 않음").isLessThan(deadline);
            Thread.sleep(100);
        }
    }

    private long createRoom(int port) {
        var created = new TestRestTemplate()
                .exchange(
                        "http://localhost:" + port + "/api/v1/rooms",
                        HttpMethod.POST,
                        new HttpEntity<>(new CreateRoom("서버 두 대 방"), headers(host)),
                        CreatedRoom.class);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        return created.getBody().roomId();
    }

    private void join(int port, long roomId) {
        var joined = new TestRestTemplate()
                .exchange(
                        "http://localhost:" + port + "/api/v1/rooms/" + roomId + "/players",
                        HttpMethod.POST,
                        new HttpEntity<>(headers(guest)),
                        Void.class);
        assertThat(joined.getStatusCode().value()).isEqualTo(201);
    }

    private HttpHeaders headers(User user) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, bearer(user));
        return headers;
    }

    private String bearer(User user) {
        return "Bearer " + jwtProvider.generateAccessToken(user.getUserId());
    }

    private static String topic(long roomId) {
        return "/topic/rooms/" + roomId;
    }

    private record Place(int to) {}

    private record CreateRoom(String title) {}

    private record CreatedRoom(long roomId, String title) {}
}
