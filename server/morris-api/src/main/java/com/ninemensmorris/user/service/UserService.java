package com.ninemensmorris.user.service;

import com.ninemensmorris.common.exception.CustomException;
import com.ninemensmorris.common.response.ErrorCode;
import com.ninemensmorris.user.domain.User;
import com.ninemensmorris.user.dto.response.MyProfileResponse;
import com.ninemensmorris.user.dto.response.NicknameResponse;
import com.ninemensmorris.user.dto.response.RankingResponse;
import com.ninemensmorris.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    // 요청자는 파라미터로 받는다. SecurityContextHolder 를 직접 읽으면
    // STOMP 스레드에서는 비어 있고 비인증이면 "anonymousUser" 가 나와 500 이 됐다
    public MyProfileResponse findMe(long userId) {
        User user = find(userId);
        return MyProfileResponse.of(user, userRepository.findRankByMmr(user.getMmr()));
    }

    public NicknameResponse findNickname(long userId) {
        return new NicknameResponse(find(userId).getNickname());
    }

    // 동점자는 같은 등수를 받고 다음 사람은 인원수만큼 건너뛴다
    // 목록 인덱스를 그대로 등수로 쓰면 findRankByMmr 이 계산한 내 등수와 어긋난다
    public List<RankingResponse> findRankings(int limit) {
        List<User> top = userRepository.findAllByOrderByMmrDescUserIdAsc(PageRequest.of(0, limit));

        List<RankingResponse> ranked = new ArrayList<>(top.size());
        int rank = 0;
        int previousMmr = Integer.MIN_VALUE;
        for (int index = 0; index < top.size(); index++) {
            User user = top.get(index);
            if (user.getMmr() != previousMmr) {
                rank = index + 1;
                previousMmr = user.getMmr();
            }
            ranked.add(RankingResponse.of(rank, user));
        }
        return ranked;
    }

    private User find(long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
