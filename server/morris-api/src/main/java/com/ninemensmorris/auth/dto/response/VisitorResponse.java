package com.ninemensmorris.auth.dto.response;

// 발급받은 토큰만 돌려준다. 프로필은 클라이언트가 /users/me 로 받아 간다
public record VisitorResponse(String accessToken) {}
