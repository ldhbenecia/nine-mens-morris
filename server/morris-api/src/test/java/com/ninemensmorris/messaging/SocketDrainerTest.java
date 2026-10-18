package com.ninemensmorris.messaging;

import static com.ninemensmorris.support.StompTestClient.next;
import static com.ninemensmorris.support.StompTestClient.subscribe;
import static org.assertj.core.api.Assertions.assertThat;

import com.ninemensmorris.game.domain.SessionTracker;
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
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompSession;

// 서버가 내려가기 전에 붙어 있는 세션에게 옮기라는 신호를 보내고, 옮겨 갈 때까지 기다리는지
class SocketDrainerTest extends IntegrationTestSupport {

    @LocalServerPort
    private int port;

    @Autowired
    private SimpMessagingTemplate messaging;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private UserRepository userRepository;

    private StompTestClient stomp;
    private User user;

    @BeforeEach
    void setUp() {
        stomp = new StompTestClient();
        user = userRepository.save(User.ofKakao("drain-" + System.nanoTime(), "이동", null));
    }

    @AfterEach
    void tearDown() {
        stomp.close();
    }

    @Test
    @DisplayName("내려가기 전에 이 서버에 붙은 세션에게 옮기라는 신호가 가고, 세션이 떠나면 바로 끝난다")
    void 옮기라고_알리고_떠날_때까지_기다린다() throws Exception {
        // given — 이 서버에 붙어 시스템 토픽을 구독한 클라이언트
        StompSession session = stomp.connect(port, "Bearer " + jwtProvider.generateAccessToken(user.getUserId()));
        BlockingQueue<ServerNotice> notices = subscribe(session, StompDestinations.SYSTEM_TOPIC, ServerNotice.class);
        Thread.sleep(300); // 구독 프레임이 브로커에 닿을 시간

        // 기다림 판정은 이 테스트가 쥔 세션 표로. 다른 테스트가 남긴 세션에 흔들리지 않게
        SessionTracker tracker = new SessionTracker();
        tracker.add(user.getUserId(), "drain-session");
        SocketDrainer drainer = new SocketDrainer(messaging, tracker);

        // when
        CompletableFuture<Void> draining = CompletableFuture.runAsync(() -> drainer.drain(Duration.ofSeconds(10)));

        // then — 신호를 받은 클라이언트가 떠나면 대기가 한도를 다 채우지 않고 끝남
        assertThat(next(notices).type()).isEqualTo("RECONNECT");
        tracker.remove("drain-session");
        draining.get(2, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("붙은 세션이 없으면 기다리지 않는다")
    void 세션이_없으면_바로_끝난다() throws Exception {
        // given
        SocketDrainer drainer = new SocketDrainer(messaging, new SessionTracker());

        // then
        CompletableFuture.runAsync(() -> drainer.drain(Duration.ofSeconds(10))).get(1, TimeUnit.SECONDS);
    }
}
