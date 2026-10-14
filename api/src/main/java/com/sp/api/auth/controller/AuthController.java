package com.sp.api.auth.controller;

import com.sp.api.auth.dto.LoginRequest;
import com.sp.api.auth.dto.LoginResponse;
import com.sp.api.auth.dto.RefreshTokenRequest;
import com.sp.api.auth.dto.SignupRequest;
import com.sp.api.auth.service.AuthService;
import com.sp.api.common.exception.TooManyRequestsException;
import com.sp.api.common.exception.UnauthorizedException;
import com.sp.api.common.response.ApiResponse;
import com.sp.api.common.security.ClientIp;
import com.sp.api.common.security.SecurityEventRecorder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SecurityEventRecorder securityEvents;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest request) {

        authService.signup(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok());
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {

        try {
            return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));

        } catch (UnauthorizedException e) {
            // 계정이 있는지는 알려 주지 않는다. 기록에도 이메일은 남기지 않는다.
            securityEvents.record(SecurityEventRecorder.Type.LOGIN_FAILED, ClientIp.of(httpRequest),
                    httpRequest.getRequestURI(), "이메일 또는 비밀번호 불일치");
            throw e;

        } catch (TooManyRequestsException e) {
            // 한 계정을 노린 실패가 쌓여 AuthService 가 잠갔다.
            securityEvents.record(SecurityEventRecorder.Type.RATE_LIMITED, ClientIp.of(httpRequest),
                    httpRequest.getRequestURI(), "한 계정 로그인 실패 한도 초과", false);
            throw e;
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {

        authService.logout(request);

        return ResponseEntity.ok(ApiResponse.ok());
    }
}
