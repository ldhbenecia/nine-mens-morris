package com.ninemensmorris.auth.service;

import com.ninemensmorris.auth.dto.response.VisitorResponse;
import com.ninemensmorris.security.JwtProvider;
import com.ninemensmorris.user.domain.User;
import com.ninemensmorris.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 로그인하지 않은 사용자에게 신원을 준다
//
// 익명으로 두지 않고 계정 행을 만드는 이유는, 신원이 없으면 방 소유권도 턴 검증도
// 소켓 인가도 걸 곳이 없기 때문이다. 로그인을 없애는 것이지 신원을 없애는 것이 아님
@Service
@RequiredArgsConstructor
@Slf4j
public class VisitorService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Transactional
    public VisitorResponse issue() {
        User visitor = userRepository.save(User.visitor(VisitorNickname.generate()));
        log.info("비로그인 계정 발급 userId={} nickname={}", visitor.getUserId(), visitor.getNickname());
        return new VisitorResponse(jwtProvider.generateVisitorToken(visitor.getUserId()));
    }
}
