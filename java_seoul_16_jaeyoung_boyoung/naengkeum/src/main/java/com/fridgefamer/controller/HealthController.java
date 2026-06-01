package com.fridgefamer.controller;

import com.fridgefamer.config.JwtProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 헬스체크 + 인증 시스템 검증 컨트롤러 (WBS-②-2 / ②-3 단계 임시).
 *
 * <p>제공 엔드포인트:
 * <ul>
 *   <li>GET /health           - DB 연결 + Flyway 마이그레이션 검증 (공개)</li>
 *   <li>GET /api/test/token   - 임의 JWT 발급 (테스트용, 공개)</li>
 *   <li>GET /api/test/me      - 인증된 사용자 정보 반환 (인증 필요)</li>
 * </ul></p>
 *
 * <p>2주차에 본격 AuthController가 만들어지면 이 컨트롤러는 정리될 예정.</p>
 */
@RestController
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final JwtProvider jwtProvider;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${spring.profiles.active}")
    private String activeProfile;

    public HealthController(JdbcTemplate jdbcTemplate, JwtProvider jwtProvider) {
        this.jdbcTemplate = jdbcTemplate;
        this.jwtProvider = jwtProvider;
    }

    // =====================================================================
    //  /health — 공개. ②-2 단계에서 만든 것 그대로
    // =====================================================================
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "OK");
        response.put("app", appName);
        response.put("profile", activeProfile);
        response.put("timestamp",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

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
        return response;
    }

    // =====================================================================
    //  /api/test/token — 공개. ②-3 검증용 임시 JWT 발급기
    // =====================================================================
    /**
     * 시드 데이터 회원(memberId=1, 닉네임=자취왕민지)으로 JWT 발급.
     * <p>2주차 AuthController가 만들어지면 이 엔드포인트는 삭제됨.</p>
     *
     * <p>호출 예시: GET /api/test/token?memberId=1&nickname=자취왕민지</p>
     */
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
        response.put("usage", "이 토큰을 Authorization: Bearer <token> 헤더로 /api/test/me 호출");
        return response;
    }

    // =====================================================================
    //  /api/test/me — 인증 필요. SecurityContext에서 로그인 회원 정보 추출
    // =====================================================================
    /**
     * 현재 인증된 회원 정보 반환.
     * <p>JwtAuthenticationFilter가 SecurityContext에 등록한 principal(memberId)을 꺼낸다.</p>
     *
     * <p>호출 시 헤더 필수: Authorization: Bearer {token}</p>
     */
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
}
