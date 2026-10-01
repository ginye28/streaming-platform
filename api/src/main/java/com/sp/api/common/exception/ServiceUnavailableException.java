package com.sp.api.common.exception;

import org.springframework.http.HttpStatus;

/** 바깥 서비스(결제사 등)를 지금 쓸 수 없다. 잠시 뒤 다시 시도하면 될 수 있다. */
public class ServiceUnavailableException extends BusinessException {

    public ServiceUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
