package com.ninemensmorris.user.domain;

// 신원을 어디서 받았는지
// VISITOR 는 로그인하지 않은 사용자. 발급처가 없으므로 providerId 가 비어 있음
// 방의 참가자를 가리키는 guest 와 섞이지 않도록 다른 낱말을 쓴다
public enum Provider {
    KAKAO,
    VISITOR
}
