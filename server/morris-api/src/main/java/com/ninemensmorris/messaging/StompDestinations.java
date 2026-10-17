package com.ninemensmorris.messaging;

import java.util.Optional;

// STOMP 목적지를 만들고 읽는 유일한 곳
// 보내는 쪽(컨트롤러·스케줄러)과 읽는 쪽(구독 검사·세션 추적·로그)이 형식을 따로 알면 한쪽만 바뀌어 어긋남
public final class StompDestinations {

    public static final String LOBBY_TOPIC = "/topic/lobby"; // 서버 → 로비 화면 전원. 방 목록이 바뀌었다는 신호
    public static final String ERROR_QUEUE = "/queue/errors"; // 서버 → 요청 보낸 사용자 한 명. 거절 사유
    public static final String SYNC_QUEUE = "/queue/sync"; // 서버 → 요청 보낸 사용자 한 명. 판 전체 상태
    public static final String USER_PREFIX = "/user/"; // 위 두 큐를 구독할 때 붙는 접두사. 사용자 한 명 전용
    public static final String APP_PREFIX = "/app/"; // 클라이언트 → 서버 컨트롤러. 착수·기권 같은 요청

    private static final String ROOM_TOPIC_PREFIX = "/topic/rooms/"; // 서버 → 그 방 참가자·관전자 전원
    private static final String APP_ROOM_PREFIX = "/app/rooms/"; // 클라이언트 → 서버. 특정 방에 대한 요청

    private StompDestinations() {}

    public static String roomTopic(long roomId) {
        return ROOM_TOPIC_PREFIX + roomId;
    }

    // /topic/rooms/{roomId} 만. 뒤에 경로가 더 붙으면 방 토픽이 아님
    // 느슨하게 읽으면 /topic/rooms/7/x 구독이 방 7 참가자 검사를 통과함
    public static Optional<Long> roomIdFromTopic(String destination) {
        if (destination == null || !destination.startsWith(ROOM_TOPIC_PREFIX)) {
            return Optional.empty();
        }
        return parseRoomId(destination.substring(ROOM_TOPIC_PREFIX.length()));
    }

    // /app/rooms/{roomId}/{행동}
    public static Optional<Long> roomIdFromAppDestination(String destination) {
        if (destination == null || !destination.startsWith(APP_ROOM_PREFIX)) {
            return Optional.empty();
        }
        String rest = destination.substring(APP_ROOM_PREFIX.length());
        int slash = rest.indexOf('/');
        return parseRoomId(slash < 0 ? rest : rest.substring(0, slash));
    }

    private static Optional<Long> parseRoomId(String text) {
        try {
            return Optional.of(Long.parseLong(text));
        } catch (NumberFormatException notARoom) {
            return Optional.empty();
        }
    }
}
