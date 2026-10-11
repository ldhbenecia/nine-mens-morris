package com.ninemensmorris;

import com.ninemensmorris.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

// 다른 통합 테스트와 같은 MySQL·Redis 컨테이너로 뜸
// 따로 MySQL 만 띄우면 Redis 를 기본 주소(localhost:6379)로 찾아, 개발 기기에 Redis 가 떠 있을 때만 통과함
class NineMensMorrisApplicationTests extends IntegrationTestSupport {

    @Test
    void contextLoads() {}
}
