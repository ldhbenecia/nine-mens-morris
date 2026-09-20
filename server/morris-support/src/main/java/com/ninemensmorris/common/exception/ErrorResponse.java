package com.ninemensmorris.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ninemensmorris.common.logging.LogContext;
import com.ninemensmorris.common.response.ErrorCode;

// 모든 실패 응답의 유일한 형태
// 예전에는 CustomException 은 {status,name,message}, 인증 실패는 {code,message},
// 검증 실패는 Spring 기본 형태로 나가서 클라이언트가 세 가지를 따로 다뤄야 했다
//
// traceId 는 호출부가 넘기지 않고 여기서 MDC 에서 꺼낸다
// 실패 응답을 만드는 곳이 다섯 군데라 인자로 받으면 한 곳만 빠뜨려도 조용히 사라진다
public record ErrorResponse(
        int status, String code, String message, @JsonInclude(JsonInclude.Include.NON_NULL) String traceId) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode, errorCode.getMessage());
    }

    // 검증 실패처럼 어디가 틀렸는지 알려줄 수 있을 때
    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.getStatus(), errorCode.name(), message, LogContext.traceId());
    }
}
