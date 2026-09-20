package com.ninemensmorris.common.logging;

import java.util.UUID;
import org.slf4j.MDC;

// 로그를 요청·사용자·방 단위로 묶는 키
// morris-support 에 둔 것은 ErrorResponse 가 같은 traceId 를 응답에 실어야 해서다
public final class LogContext {

    public static final String TRACE_ID = "traceId";
    public static final String USER_ID = "userId";
    public static final String ROOM_ID = "roomId";

    private static final int TRACE_ID_LENGTH = 8;

    private LogContext() {}

    public static String startTrace() {
        String traceId = UUID.randomUUID().toString().substring(0, TRACE_ID_LENGTH);
        MDC.put(TRACE_ID, traceId);
        return traceId;
    }

    public static void putUserId(long userId) {
        MDC.put(USER_ID, String.valueOf(userId));
    }

    public static void putRoomId(long roomId) {
        MDC.put(ROOM_ID, String.valueOf(roomId));
    }

    public static String traceId() {
        return MDC.get(TRACE_ID);
    }

    // 스레드가 재사용되므로 반드시 finally 에서 부른다. 안 지우면 다음 작업에 남의 값이 붙는다
    public static void clear() {
        MDC.remove(TRACE_ID);
        MDC.remove(USER_ID);
        MDC.remove(ROOM_ID);
    }
}
