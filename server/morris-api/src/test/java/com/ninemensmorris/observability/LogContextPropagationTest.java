package com.ninemensmorris.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.ninemensmorris.common.logging.LogContext;
import com.ninemensmorris.support.IntegrationTestSupport;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.TaskScheduler;

// traceId 가 실제로 붙는지 확인한다
// 로그 출력은 검증하기 번거로우므로, 같은 MDC 를 쓰는 에러 응답과 스케줄러 스레드로 확인한다
class LogContextPropagationTest extends IntegrationTestSupport {

    private final TestRestTemplate rest = new TestRestTemplate();

    @LocalServerPort
    private int port;

    @Autowired
    private TaskScheduler taskScheduler;

    @Test
    @DisplayName("HTTP 실패 응답에 traceId 가 실린다")
    void 실패_응답에_traceId_가_실린다() {
        // when — 인증 없이 보호된 자원에 접근하면 401 이 나간다
        String body = rest.exchange("http://localhost:" + port + "/api/v1/users/me", HttpMethod.GET, null, String.class)
                .getBody();

        // then — 사용자가 이 값을 알려주면 그것만으로 로그를 특정할 수 있다
        assertThat(body).contains("\"traceId\"");
    }

    @Test
    @DisplayName("스케줄러 스레드에도 traceId 가 붙고 실행이 끝나면 지워진다")
    void 스케줄러_스레드에_traceId_가_붙는다() throws Exception {
        // given — 재접속 유예와 유휴 방 정리가 이 스케줄러에서 돌고 게임을 정산한다
        AtomicReference<String> insideTask = new AtomicReference<>();
        AtomicReference<String> afterTask = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        // when — 재접속 유예가 쓰는 것과 같은 경로(schedule)로 넣는다
        taskScheduler.schedule(
                () -> {
                    insideTask.set(LogContext.traceId());
                    done.countDown();
                },
                Instant.now());
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();

        // then
        assertThat(insideTask.get()).isNotBlank();

        // 같은 스레드가 재사용되므로 다음 작업에 남아 있으면 안 된다
        CountDownLatch second = new CountDownLatch(1);
        taskScheduler.schedule(
                () -> {
                    afterTask.set(LogContext.traceId());
                    second.countDown();
                },
                Instant.now());
        assertThat(second.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(afterTask.get()).isNotBlank().isNotEqualTo(insideTask.get());
    }
}
