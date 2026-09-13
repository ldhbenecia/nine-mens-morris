package com.ninemensmorris.user.controller;

import com.ninemensmorris.security.AuthenticatedUser;
import com.ninemensmorris.user.dto.response.MyProfileResponse;
import com.ninemensmorris.user.dto.response.NicknameResponse;
import com.ninemensmorris.user.dto.response.RankingResponse;
import com.ninemensmorris.user.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

    private static final int MAX_RANKING_SIZE = 100;

    private final UserService userService;

    @GetMapping("/users/me")
    public MyProfileResponse findMe(AuthenticatedUser actor) {
        return userService.findMe(actor.id());
    }

    @GetMapping("/users/{userId}")
    public NicknameResponse findNickname(@PathVariable long userId) {
        return userService.findNickname(userId);
    }

    // 위쪽만 막아서 limit=-1 이면 PageRequest.of 가 터졌음. 아래쪽도 잠금
    @GetMapping("/rankings")
    public List<RankingResponse> findRankings(@RequestParam(defaultValue = "100") int limit) {
        return userService.findRankings(Math.clamp(limit, 1, MAX_RANKING_SIZE));
    }
}
