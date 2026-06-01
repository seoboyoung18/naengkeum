package com.fridgefamer.controller;

import com.fridgefamer.config.JwtProvider;
import com.fridgefamer.exception.ApiException;
import com.fridgefamer.exception.ErrorCode;
import com.fridgefamer.mapper.system.HealthMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 헬스체크 + 인증/예외/MyBatis 시스템 검증 컨트롤러.
 *
 * <p>WBS-②-2~②-5 단계 임시. 2주차 본격 컨트롤러 작성 시 정리될 예정.</p>
 *
 * <p>제공 엔드포인트:
 * <ul>
 *   <li>GET  /health                       - DB + Flyway + MyBatis 검증 (공개)</li>
 *   <li>GET  /api/test/token               - 임시 JWT 발급 (공개)</li>
 *   <li>GET  /api/test/me                  - 인증된 사용자 정보 (인증 필요)</li>
 *   <li>GET  /api/test/error/api           - ApiException 테스트</li>
 *   <li>POST /api/test/error/validation    - @Valid Body 검증 테스트</li>
 *   <li>GET  /api/test/error/path/{id}     - 타입 변환 실패 테스트</li>
 *   <li>GET  /api/test/error/unexpected    - Fallback 500 테스트</li>
 * </ul></p>
 */
@RestController
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final JwtProvider jwtProvider;
    private final HealthMapper healthMapper;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${spring.profiles.active}")
    private String activeProfile;

    public HealthController(JdbcTemplate jdbcTemplate,
                            JwtProvider jwtProvider,
                            HealthMapper healthMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.jwtProvider = jwtProvider;
        this.healthMapper = healthMapper;
    }

    // =====================================================================
    //  /health — DB(JdbcTemplate) + MyBatis 동작 검증
    // =====================================================================
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "OK");
        response.put("app", appName);
        response.put("profile", activeProfile);
        response.put("timestamp",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // ----- 1. JdbcTemplate 검증 (직접 SQL) -----
        try {
            Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            Integer tableCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()",
                    Integer.class);
            Integer badgeCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM badge", Integer.class);

            Map<String, Object> db = new LinkedHashMap<>();
            db.put("ping", one);
            db.put("tableCount", tableCount);
            db.put("badgeCount", badgeCount);
            response.put("db", db);
        } catch (Exception e) {
            response.put("status", "DB_ERROR");
            response.put("error", e.getMessage());
        }

        // ----- 2. MyBatis 검증 (XML Mapper) -----
        try {
            Map<String, Object> mybatis = new LinkedHashMap<>();
            mybatis.put("memberCount", healthMapper.countMembers());
            mybatis.put("fridgeItems", healthMapper.countFridgeItems());
            mybatis.put("expiringSoonD3", healthMapper.countExpiringSoon());

            // snake_case → camelCase 자동 매핑 검증
            Map<String, Object> firstMember = healthMapper.findFirstMember();
            if (firstMember != null) {
                mybatis.put("firstMemberNickname", firstMember.get("nickname"));
                mybatis.put("firstMemberId", firstMember.get("memberId"));   // camelCase!
            }

            response.put("mybatis", mybatis);
        } catch (Exception e) {
            response.put("mybatisStatus", "ERROR");
            response.put("mybatisError", e.getMessage());
        }

        return response;
    }

    // =====================================================================
    //  /api/test/token — 임시 JWT 발급
    // =====================================================================
    @GetMapping("/api/test/token")
    public Map<String, Object> issueTestToken(
            @RequestParam(defaultValue = "1") Long memberId,
            @RequestParam(defaultValue = "자취왕민지") String nickname,
            @RequestParam(defaultValue = "false") boolean rememberMe) {

        String token = jwtProvider.createToken(memberId, nickname, rememberMe);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("token", token);
        response.put("memberId", memberId);
        response.put("nickname", nickname);
        response.put("rememberMe", rememberMe);
        return response;
    }

    // =====================================================================
    //  /api/test/me — 인증된 사용자 정보 반환
    // =====================================================================
    @GetMapping("/api/test/me")
    public Map<String, Object> getMyInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long memberId = (Long) auth.getPrincipal();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("authenticated", true);
        response.put("memberId", memberId);
        response.put("message", "JWT 인증 성공!");
        return response;
    }

    // =====================================================================
    //  ②-4 검증용 에러 테스트 엔드포인트들
    // =====================================================================

    @GetMapping("/api/test/error/api")
    public void throwApiException(
            @RequestParam(defaultValue = "NOT_FOUND") String code) {
        ErrorCode errorCode = ErrorCode.valueOf(code);
        throw new ApiException(errorCode);
    }

    @PostMapping("/api/test/error/validation")
    public Map<String, Object> validateBody(@Valid @RequestBody TestRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", true);
        response.put("received", request);
        return response;
    }

    @GetMapping("/api/test/error/path/{id}")
    public Map<String, Object> typeMismatch(@PathVariable Long id) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", true);
        response.put("id", id);
        return response;
    }

    @GetMapping("/api/test/error/unexpected")
    public void throwUnexpected() {
        throw new RuntimeException("의도적으로 던진 예상 못한 에러");
    }

    /** Validation 검증용 내부 DTO */
    public record TestRequest(
            @NotBlank(message = "이메일은 필수입니다")
            @Email(message = "올바른 이메일 형식이 아닙니다")
            String email,

            @NotBlank(message = "비밀번호는 필수입니다")
            @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다")
            String password,

            @NotBlank(message = "닉네임은 필수입니다")
            @Size(min = 2, max = 10, message = "닉네임은 2~10자여야 합니다")
            String nickname
    ) {}
}