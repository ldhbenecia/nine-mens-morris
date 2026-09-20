package com.ninemensmorris.auth.service;

import com.ninemensmorris.auth.domain.CustomOAuth2User;
import com.ninemensmorris.user.domain.Provider;
import com.ninemensmorris.user.domain.User;
import com.ninemensmorris.user.repository.UserRepository;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final String KAKAO = "kakao";

    // users.nickname 컬럼 길이와 같아야 함
    private static final int MAX_NICKNAME_LENGTH = 20;

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // getClientName 은 표시 이름이라 설정에서 바뀔 수 있음. 등록 ID 로 판별함
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        if (!KAKAO.equals(registrationId)) {
            throw new OAuth2AuthenticationException("지원하지 않는 OAuth2 프로바이더: " + registrationId);
        }

        String providerId = oAuth2User.getAttribute("id").toString();
        User user = userRepository
                .findByProviderAndProviderId(Provider.KAKAO, providerId)
                .orElseGet(() -> userRepository.save(toUser(oAuth2User, providerId)));
        syncProfile(user, oAuth2User, providerId);

        // 토큰 subject 는 카카오 회원번호가 아니라 서비스가 발급한 식별자다
        return new CustomOAuth2User(user.getUserId());
    }

    private void syncProfile(User user, OAuth2User oAuth2User, String providerId) {
        Map<String, Object> properties = oAuth2User.getAttribute("properties");
        if (properties == null) {
            return;
        }

        String nickname = nicknameOf((String) properties.get("nickname"), providerId);
        String imageUrl = (String) properties.get("profile_image");
        if (nickname.equals(user.getNickname()) && Objects.equals(imageUrl, user.getImageUrl())) {
            return;
        }
        user.changeProfile(nickname, imageUrl);
    }

    private User toUser(OAuth2User oAuth2User, String providerId) {
        Map<String, Object> properties = oAuth2User.getAttribute("properties");

        String nickname = properties == null ? null : (String) properties.get("nickname");
        String profileImage = properties == null ? null : (String) properties.get("profile_image");

        return User.ofKakao(providerId, nicknameOf(nickname, providerId), profileImage);
    }

    // 닉네임 동의를 안 했으면 회원번호로 만들어 준다
    // 컬럼이 20자라 그냥 이어 붙이면 회원번호가 길 때 저장에서 터진다
    private String nicknameOf(String nickname, String providerId) {
        if (nickname != null && !nickname.isBlank()) {
            return truncate(nickname);
        }
        return truncate("플레이어" + providerId);
    }

    private String truncate(String value) {
        return value.length() > MAX_NICKNAME_LENGTH ? value.substring(0, MAX_NICKNAME_LENGTH) : value;
    }
}
