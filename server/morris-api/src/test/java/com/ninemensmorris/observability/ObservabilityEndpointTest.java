package com.ninemensmorris.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.ninemensmorris.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

// 관측 엔드포인트는 설정으로만 생기므로 요청을 보내 봐야 살아 있는지 알 수 있다
// 인가 기본값이 denyAll 이라 열어두는 것을 빠뜨리면 프로브가 401 을 받고 파드가 재시작을 반복한다
//
// Spring Boot Test 는 테스트에서 simple 외의 메트릭 익스포터를 전부 끈다
// 그래서 prometheus 만 다시 켜 준다. 운영 설정과는 무관함
@TestPropertySource(properties = "management.prometheus.metrics.export.enabled=true")
class ObservabilityEndpointTest extends IntegrationTestSupport {

    private final TestRestTemplate rest = new TestRestTemplate();

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("프로브 두 개가 인증 없이 열려 있다")
    void 프로브가_열려있다() {
        // then — 토큰 없이 200 이어야 한다. kubelet 은 토큰을 들고 오지 않는다
        assertThat(get("/actuator/health/liveness").getStatusCode().value()).isEqualTo(200);
        assertThat(get("/actuator/health/readiness").getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @DisplayName("readiness 에는 DB 와 게임 세션이 들어가고 liveness 에는 들어가지 않는다")
    void readiness_에만_외부_의존성이_들어간다() {
        // given — show-details 가 never 라 본문으로는 구성요소를 알 수 없다
        // 대신 DB 가 살아 있는 지금 둘 다 UP 인지로 그룹 구성이 유효한지 확인한다
        // 그룹에 없는 이름을 넣었다면 기동 자체가 실패하므로 이 테스트가 그것도 잡는다
        ResponseEntity<String> liveness = get("/actuator/health/liveness");
        ResponseEntity<String> readiness = get("/actuator/health/readiness");

        // then
        assertThat(liveness.getBody()).contains("UP");
        assertThat(readiness.getBody()).contains("UP");
    }

    @Test
    @DisplayName("프로메테우스 엔드포인트에 게임 메트릭과 공통 태그가 나온다")
    void 게임_메트릭이_노출된다() {
        // when
        ResponseEntity<String> response = get("/actuator/prometheus");

        // then — 이름이 바뀌면 대시보드와 알림이 조용히 빈 그래프가 된다
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        String body = response.getBody();
        assertThat(body).contains("morris_rooms_active");
        assertThat(body).contains("morris_sockets_open");
        // 공통 태그가 안 붙으면 여러 환경의 메트릭을 구분할 수 없다
        assertThat(body).contains("application=\"nine-mens-morris\"");
        assertThat(body).contains("environment=");
    }

    @Test
    @DisplayName("설정값이 드러나는 엔드포인트는 열려 있지 않다")
    void 민감한_엔드포인트는_닫혀있다() {
        // then — exposure 에 넣지 않았으므로 404, 인가에서 막히면 401
        assertThat(get("/actuator/env").getStatusCode().value()).isNotEqualTo(200);
        assertThat(get("/actuator/beans").getStatusCode().value()).isNotEqualTo(200);
    }

    private ResponseEntity<String> get(String path) {
        return rest.exchange("http://localhost:" + port + path, HttpMethod.GET, null, String.class);
    }
}
