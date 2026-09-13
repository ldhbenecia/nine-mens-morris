package com.ninemensmorris.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;

// 통합 테스트 공통 컨테이너
//
// H2 로 대체하면 방언 차이로 운영에서만 터지는 문제가 생김
//
// @Testcontainers + @Container 는 클래스 단위 생명주기라
// 통합 테스트 클래스 수만큼 MySQL 을 껐다 켠다
// 직접 start() 해서 실행당 한 번만 띄운다. 종료는 Ryuk 이 JVM 종료 때 정리함
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTestSupport {

    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    static {
        MYSQL.start();
    }
}
