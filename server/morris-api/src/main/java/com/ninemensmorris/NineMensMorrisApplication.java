package com.ninemensmorris;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// 유휴 방 정리와 소켓 재접속 유예에 스케줄러가 필요함
// 이게 없으면 @Scheduled 가 안 돌고 TaskScheduler 빈도 생기지 않음
@EnableScheduling
@SpringBootApplication
public class NineMensMorrisApplication {

    public static void main(String[] args) {
        SpringApplication.run(NineMensMorrisApplication.class, args);
    }
}
