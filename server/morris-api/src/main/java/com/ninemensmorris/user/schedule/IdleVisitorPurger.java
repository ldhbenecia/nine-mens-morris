package com.ninemensmorris.user.schedule;

import com.ninemensmorris.user.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 오래된 비로그인 계정 정리
//
// 보존 기간은 토큰 수명보다 충분히 길어야 한다
// 토큰이 살아 있는 계정을 지우면 게임 도중에 신원이 사라져 연결이 끊긴다
// 지금은 토큰 7일 / 보존 30일이라 지워지는 행의 토큰은 이미 만료된 지 오래다
@Component
@RequiredArgsConstructor
@Slf4j
public class IdleVisitorPurger {

    private static final Duration RETENTION = Duration.ofDays(30);

    private final UserRepository userRepository;

    // 사용량이 가장 적은 새벽에 하루 한 번
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void purge() {
        int deleted = userRepository.deleteVisitorsCreatedBefore(Instant.now().minus(RETENTION));
        if (deleted > 0) {
            log.info("유휴 비로그인 계정 정리 완료. 삭제 {}건", deleted);
        }
    }
}
