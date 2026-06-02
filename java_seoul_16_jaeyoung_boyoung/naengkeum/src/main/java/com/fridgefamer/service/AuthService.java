package com.fridgefamer.service;

import com.fridgefamer.config.JwtProvider;
import com.fridgefamer.dto.request.auth.LoginRequest;
import com.fridgefamer.dto.request.auth.RegisterRequest;
import com.fridgefamer.dto.response.auth.EmailAvailableResponse;
import com.fridgefamer.dto.response.auth.LoginResponse;
import com.fridgefamer.dto.response.auth.MemberAuthRow;
import com.fridgefamer.dto.response.auth.RegisterResponse;
import com.fridgefamer.dto.response.auth.TokenVerifyResponse;
import com.fridgefamer.exception.ApiException;
import com.fridgefamer.exception.ErrorCode;
import com.fridgefamer.mapper.member.MemberMapper;
import com.fridgefamer.mapper.member.MemberMapper.RegisterCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 인증 도메인 서비스 — 회원가입/로그인/이메일 중복 확인/토큰 검증.
 *
 * <p>비밀번호 인코딩은 SecurityConfig가 빈으로 제공하는 BCryptPasswordEncoder(strength=10) 사용.
 * JWT 발급은 JwtProvider 위임. 도메인 예외는 ApiException + ErrorCode로 통일.</p>
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthService(MemberMapper memberMapper,
                       PasswordEncoder passwordEncoder,
                       JwtProvider jwtProvider) {
        this.memberMapper = memberMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    // =====================================================================
    //  POST /api/auth/register
    // =====================================================================
    @Transactional
    public RegisterResponse register(RegisterRequest req) {
        // 1. 사전 중복 확인 — 사용자에게 어떤 필드가 충돌인지 메시지로 알려주기 위함.
        //    DB UNIQUE는 마지막 방어선(DuplicateKeyException → 409).
        if (memberMapper.countByEmail(req.email()) > 0) {
            throw new ApiException(ErrorCode.CONFLICT, "이미 사용 중인 이메일입니다");
        }
        if (memberMapper.countByNickname(req.nickname()) > 0) {
            throw new ApiException(ErrorCode.CONFLICT, "이미 사용 중인 닉네임입니다");
        }

        // 2. 비밀번호 해시 (BCrypt strength=10 — SecurityConfig 빈)
        String hash = passwordEncoder.encode(req.password());

        // 3. allergies 직렬화 — DB는 VARCHAR(255) 콤마 구분 (스키마 member.allergies)
        String allergiesCsv = serializeAllergies(req.allergies());

        boolean marketing = Boolean.TRUE.equals(req.marketingAgree());

        RegisterCommand cmd = new RegisterCommand(
                req.email(), hash, req.nickname(), allergiesCsv, marketing);
        memberMapper.insertMember(cmd);

        Long memberId = cmd.getMemberId();
        log.info("Registered member: id={}, email={}", memberId, req.email());
        return new RegisterResponse(memberId);
    }

    // =====================================================================
    //  POST /api/auth/login
    // =====================================================================
    public LoginResponse login(LoginRequest req) {
        MemberAuthRow row = memberMapper.findAuthByEmail(req.email());

        // 존재하지 않는 이메일은 일반적인 401 — 이메일 enumeration 방지 위해 메시지 통일
        if (row == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다");
        }

        // 비밀번호 검증
        if (!passwordEncoder.matches(req.password(), row.password())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다");
        }

        // 탈퇴 회원 — API 명세 §1 로그인 Error 403
        if (!row.isActive()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "탈퇴한 회원입니다");
        }

        boolean rememberMe = Boolean.TRUE.equals(req.rememberMe());
        String token = jwtProvider.createToken(row.memberId(), row.nickname(), rememberMe);

        return new LoginResponse(token, row.nickname(), row.memberId());
    }

    // =====================================================================
    //  GET /api/auth/check-email
    //  중복이어도 200 + {available:false} (API 명세 2026-05-29 결정)
    // =====================================================================
    public EmailAvailableResponse checkEmail(String email) {
        boolean available = memberMapper.countByEmail(email) == 0;
        return new EmailAvailableResponse(available);
    }

    // =====================================================================
    //  GET /api/auth/verify
    //  JwtAuthenticationFilter가 통과시킨 시점에 호출되므로 토큰은 이미 유효.
    //  (만료/위조는 SecurityConfig가 401로 응답)
    // =====================================================================
    public TokenVerifyResponse verify() {
        return new TokenVerifyResponse(true);
    }

    // =====================================================================
    //  내부 헬퍼
    // =====================================================================
    private String serializeAllergies(List<String> allergies) {
        if (allergies == null || allergies.isEmpty()) {
            return null;
        }
        return String.join(",", allergies.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .toList());
    }
}
