package com.sp.api.common.exception;

import org.springframework.http.HttpStatus;

/** 짧은 시간에 너무 많이 시도했다. 잠시 뒤 다시 하면 된다. */
public class TooManyRequestsException extends BusinessException {

    public TooManyRequestsException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, message);
    }
}
