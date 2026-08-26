package com.sp.api.common.exception;

import com.sp.api.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

/**
 * ResponseEntityExceptionHandler 를 상속해 Spring MVC 표준 예외(400/404/405 등)의
 * 상태 코드를 유지하면서, 본문만 ApiResponse 형태로 통일한다.
 * 상속하지 않고 Exception catch-all 만 두면 그 예외들이 전부 500 으로 바뀐다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {

        return ResponseEntity.status(e.getStatus())
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {

        return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }

    /** 예상하지 못한 예외는 원인을 로그에만 남기고 클라이언트에는 노출하지 않는다. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {

        log.error("처리되지 않은 예외", e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("서버 오류가 발생했습니다."));
    }

    /**
     * `@Validated` 가 붙은 컨트롤러의 파라미터 검증(@RequestParam 의 @NotBlank·@Size 등)은
     * MethodValidationInterceptor 를 거쳐 이 예외로 온다. 여기서 잡지 않으면
     * 아래 catch-all 로 떨어져 잘못된 요청이 500 이 된다.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {

        String message = e.getConstraintViolations()
                .stream()
                .map(violation -> lastNode(violation.getPropertyPath().toString())
                        + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));

        if (message.isBlank()) {
            message = "요청 값이 올바르지 않습니다.";
        }

        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    /** 경로가 "search.keyword" 처럼 오므로 파라미터 이름만 남긴다. */
    private static String lastNode(String propertyPath) {
        return propertyPath.substring(propertyPath.lastIndexOf('.') + 1);
    }

    /** @Valid 실패는 어떤 필드가 왜 틀렸는지 알려준다. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        if (message.isBlank()) {
            message = "요청 값이 올바르지 않습니다.";
        }

        return ResponseEntity.status(status).body(ApiResponse.error(message));
    }

    /** 부모가 처리하는 나머지 표준 예외들의 본문을 ApiResponse 로 감싼다. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {

        if (statusCode.is5xxServerError()) {
            log.error("서버 오류", ex);
        }

        return ResponseEntity.status(statusCode)
                .headers(headers)
                .body(ApiResponse.error(resolveMessage(ex, body, statusCode)));
    }

    private String resolveMessage(Exception ex, Object body, HttpStatusCode statusCode) {

        if (statusCode.is5xxServerError()) {
            return "서버 오류가 발생했습니다.";
        }

        // 부모가 413 으로 매핑하지만 기본 메시지가 불친절하다.
        if (ex instanceof MaxUploadSizeExceededException) {
            return "업로드 가능한 파일 크기를 초과했습니다.";
        }

        if (body instanceof ProblemDetail problemDetail && problemDetail.getDetail() != null) {
            return problemDetail.getDetail();
        }

        return ex.getMessage() != null ? ex.getMessage() : "요청을 처리할 수 없습니다.";
    }
}
