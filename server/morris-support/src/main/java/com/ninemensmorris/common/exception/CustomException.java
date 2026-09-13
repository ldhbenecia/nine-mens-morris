package com.ninemensmorris.common.exception;

import com.ninemensmorris.common.response.ErrorCode;
import lombok.Getter;

// super(message) 를 부르지 않으면 getMessage() 가 null 이어서
// 로그나 스택트레이스에 무슨 오류인지 전혀 남지 않는다
@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
