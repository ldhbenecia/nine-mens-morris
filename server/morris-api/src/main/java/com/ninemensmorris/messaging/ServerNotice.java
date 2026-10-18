package com.ninemensmorris.messaging;

// 판이 아니라 연결에 관한 서버 알림. 이 서버에 붙은 세션에게만 감
public record ServerNotice(String type) {

    // 이 서버가 곧 내려감. 다른 서버에 새 연결을 먼저 붙인 뒤 이 연결을 닫으라는 뜻
    public static final ServerNotice RECONNECT = new ServerNotice("RECONNECT");
}
