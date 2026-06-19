package com.fridgefamer.config;

import com.fridgefamer.config.oauth.CustomOAuth2UserService;
import com.fridgefamer.config.oauth.HttpCookieOAuth2AuthorizationRequestRepository;
import com.fridgefamer.config.oauth.OAuth2SuccessHandler;
import com.fridgefamer.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

/**
 * Spring Security 설정 — WBS-②-3 본격 버전.
 *
 * <p>
 * 핵심 정책:
 * <ul>
 * <li>Stateless (세션 미사용, JWT 기반)</li>
 * <li>CSRF 비활성화 (JWT라 불필요)</li>
 * <li>CORS 허용 (Vue dev 서버 localhost:5173)</li>
 * <li>공개/보호 경로 분리 (API 명세서 기준)</li>
 * <li>인증 실패 시 9종 에러 코드 JSON 응답</li>
 * <li>BCryptPasswordEncoder 빈 등록 (회원가입/로그인용)</li>
 * </ul>
 * </p>
 *
 * <p>
 * 응답 JSON은 단순한 2-필드 구조라 ObjectMapper 의존성 없이 수동 작성.
 * Jackson 자동 빈 등록이 환경에 따라 달라지는 문제를 회피한다.
 * </p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          CustomOAuth2UserService customOAuth2UserService,
                          OAuth2SuccessHandler oAuth2SuccessHandler,
                          HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.cookieAuthorizationRequestRepository = cookieAuthorizationRequestRepository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // ----- CSRF: JWT stateless API는 불필요 -----
                .csrf(csrf -> csrf.disable())

                // ----- CORS: Vue dev 서버 호출 허용 -----
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ----- Session: 사용하지 않음 (모든 요청은 JWT로 인증) -----
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ----- Basic/Form Login 비활성화 -----
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable())

                // ----- 경로별 접근 권한 -----
                .authorizeHttpRequests(auth -> auth
                        // SSE(SseEmitter) 등 비동기 응답이 끝나고 ASYNC/ERROR 디스패치로
                        // 되돌아올 때 SecurityContext가 비어 재인가에 걸리는 문제 방지.
                        // (없으면 SSE 스트림이 끝에 Access Denied로 비정상 종료 → 브라우저 "network error")
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()

                        // 관리자 전용 — 모든 다른 규칙보다 먼저, ROLE_ADMIN만 허용
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 레시피 쓰기/내 것 조회는 인증 필요 — 아래 "/api/recipe/**" permitAll보다 먼저 선언해 우선 매칭
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/recipe/from-ai", "/api/recipe/from-ai/**").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/recipe/*/publish").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/recipe/mine").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/recipe/*/image").authenticated()
                        // 레시피 삭제(본인/관리자)는 인증 필요 — 아래 permitAll보다 먼저 선언
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/recipe/*").authenticated()

                        // 공개 경로
                        .requestMatchers(
                                "/health", // 헬스체크
                                "/actuator/health", // 컨테이너/모니터링 헬스체크
                                "/error",
                                "/images/**", // 업로드된 레시피 이미지 (정적 서빙)
                                "/api/test/token", // 임시 JWT 발급
                                "/api/test/error/**", // (②-3 테스트용)
                                "/api/auth/login", // 로그인
                                "/api/auth/register", // 회원가입
                                "/api/auth/check-email", // 이메일 중복 확인
                                "/oauth2/**", // 소셜 로그인 시작(authorize 리다이렉트)
                                "/login/oauth2/**", // 소셜 로그인 콜백(code 교환)
                                "/api/recipe/**", // 레시피 검색/상세 (조회만)
                                "/api/ingredients/**", // 식재료 사전 자동완성/제안 (조회만)
                                "/api/challenge", // 챌린지 목록 (조회)
                                "/api/challenge/stats", // 챌린지 통계
                                "/api/member/*/profile", // 타 유저 프로필
                                "/api/member/*/recipes" // 타 유저 공개 레시피 목록
                        ).permitAll()

                        // 리뷰 목록 조회는 GET만 공개 (POST/PUT/DELETE는 인증 필요)
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/review").permitAll()

                        // 챌린지: /my는 인증 필요 — 상세 공개 규칙보다 먼저 선언해 우선 매칭
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/challenge/my").authenticated()
                        // 챌린지 상세는 GET만 공개 (/{id} 한 세그먼트 — /{id}/join은 두 세그먼트라 인증 유지)
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/challenge/*").permitAll()

                        // OPTIONS preflight 허용 (CORS)
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()

                        // 나머지는 모두 인증 필요
                        .anyRequest().authenticated())

                // ----- 소셜 로그인 (구글 / 카카오) -----
                // STATELESS라 인가요청은 쿠키 저장소(cookieAuthorizationRequestRepository)로 보관.
                // 성공 시 우리 JWT를 발급해 프론트로 리다이렉트, 실패 시 프론트 로그인으로 복귀.
                .oauth2Login(oauth -> oauth
                        .authorizationEndpoint(ae -> ae
                                .authorizationRequestRepository(cookieAuthorizationRequestRepository))
                        .userInfoEndpoint(ui -> ui.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler((req, res, ex) -> {
                            // 실패 원인을 로그로 남겨 디버깅 가능하게(토큰 교환/응답 파싱 등).
                            log.warn("소셜 로그인 실패: {}", ex.getMessage(), ex);
                            cookieAuthorizationRequestRepository.removeAuthorizationRequestCookies(req, res);
                            res.sendRedirect(frontendBaseUrl + "/login?error=oauth");
                        }))

                // ----- 인증 실패/권한 부족 시 통일된 JSON 응답 -----
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(this::onAuthenticationFailure) // 401
                        .accessDeniedHandler((req, res, ex) -> writeError(res, ErrorCode.FORBIDDEN)))

                // ----- JWT 필터를 표준 인증 필터 앞에 등록 -----
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 인증 실패 시 (401).
     * JwtAuthenticationFilter가 request.setAttribute로 남긴 jwtErrorCode를 확인하여
     * 만료(TOKEN_EXPIRED) / 위조(INVALID_TOKEN) / 부재(UNAUTHORIZED) 구분.
     */
    private void onAuthenticationFailure(HttpServletRequest request,
            HttpServletResponse response,
            org.springframework.security.core.AuthenticationException ex)
            throws IOException {
        Object marker = request.getAttribute("jwtErrorCode");
        ErrorCode code;
        if ("TOKEN_EXPIRED".equals(marker)) {
            code = ErrorCode.TOKEN_EXPIRED;
        } else if ("INVALID_TOKEN".equals(marker)) {
            code = ErrorCode.INVALID_TOKEN;
        } else {
            code = ErrorCode.UNAUTHORIZED;
        }
        writeError(response, code);
    }

    /**
     * 9종 에러 코드 응답 헬퍼.
     *
     * <p>
     * 단순한 2-필드 JSON 응답이라 ObjectMapper 없이 수동 작성.
     * 향후 GlobalExceptionHandler(②-4)에서는 정식 ObjectMapper 사용.
     * </p>
     */
    private void writeError(HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // 안전한 JSON 수동 작성. message는 따옴표 이스케이프 처리.
        String json = String.format(
                "{\"code\":\"%s\",\"message\":\"%s\"}",
                code.getCode(),
                code.getDefaultMessage().replace("\"", "\\\""));
        response.getWriter().write(json);
    }

    /** CORS — Vue dev 서버(5173) 및 운영 도메인 허용 */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of(
                "http://localhost:5173", // Vue dev (Vite 기본)
                "http://localhost:3000", // 혹시 React/다른 dev 서버
                "https://*.fridgefamer.com"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("Authorization"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    /**
     * 비밀번호 인코더 — BCrypt 사용.
     * <p>
     * SSAFY 시드 데이터의 'Test1234!' 비밀번호도 strength=10 BCrypt 해시로 생성되어 있음.
     * AuthService에서 회원가입/로그인 시 이 빈을 주입받아 사용.
     * </p>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // 기본 strength=10
    }
}