package com.fridgefamer.controller;

import com.fridgefamer.dto.request.auth.LoginRequest;
import com.fridgefamer.dto.request.auth.RegisterRequest;
import com.fridgefamer.dto.response.auth.EmailAvailableResponse;
import com.fridgefamer.dto.response.auth.LoginResponse;
import com.fridgefamer.dto.response.auth.RegisterResponse;
import com.fridgefamer.dto.response.auth.TokenVerifyResponse;
import com.fridgefamer.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증(Auth) API — API 명세 §1.
 *
 * <ul>
 *   <li>POST /api/auth/register     — F10 회원가입</li>
 *   <li>POST /api/auth/login        — F14 로그인</li>
 *   <li>GET  /api/auth/check-email  — F10 이메일 중복 확인</li>
 *   <li>GET  /api/auth/verify       — Splash 토큰 유효성 확인 (인증 필요)</li>
 * </ul>
 *
 * <p>register/login/check-email은 SecurityConfig에서 permitAll, verify는 인증 필요.</p>
 */
@RestController
@RequestMapping("/api/auth")
@Validated   // @RequestParam(@Email 등) 검증 트리거
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest req) {
        RegisterResponse body = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @GetMapping("/check-email")
    public EmailAvailableResponse checkEmail(
            @RequestParam
            @NotBlank(message = "이메일은 필수입니다")
            @Email(message = "올바른 이메일 형식이 아닙니다")
            String email
    ) {
        return authService.checkEmail(email);
    }

    @GetMapping("/verify")
    public TokenVerifyResponse verify() {
        // SecurityFilter가 통과시켰다면 토큰은 유효한 상태.
        // 만료/위조는 SecurityConfig.onAuthenticationFailure 에서 401 처리.
        return authService.verify();
    }
}
