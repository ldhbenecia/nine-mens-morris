package com.ninemensmorris.auth.controller;

import com.ninemensmorris.auth.dto.response.VisitorResponse;
import com.ninemensmorris.auth.service.VisitorIssueLimiter;
import com.ninemensmorris.auth.service.VisitorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final VisitorService visitorService;
    private final VisitorIssueLimiter visitorIssueLimiter;

    // 요청 본문이 없다. 닉네임까지 서버가 정하므로 클라이언트가 보낼 것이 없음
    // 호출자 IP 는 웹 계층에서만 알 수 있으므로 한도 확인도 여기서 한다
    @PostMapping("/visitors")
    @ResponseStatus(HttpStatus.CREATED)
    public VisitorResponse issueVisitor(HttpServletRequest request) {
        visitorIssueLimiter.check(request.getRemoteAddr());
        return visitorService.issue();
    }
}
