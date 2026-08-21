package com.sp.api.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 클라이언트에게 그대로 노출해도 되는 예상된 실패를 나타낸다.
 * 하위 타입이 각자 HTTP 상태를 결정한다.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    protected BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
