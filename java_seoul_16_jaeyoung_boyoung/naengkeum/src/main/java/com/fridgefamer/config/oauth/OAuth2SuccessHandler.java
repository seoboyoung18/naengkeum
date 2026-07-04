package com.fridgefamer.config.oauth;

import com.fridgefamer.config.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

/**
 * 소셜 로그인 성공 후, 우리 서비스의 JWT를 발급하여 프론트엔드로 리다이렉트한다.
 *
 * <p>토큰은 URL fragment(#token=...)로 전달한다. 쿼리스트링과 달리 서버 접근/리퍼러
 * 로그에 남지 않아 노출 위험이 낮고, 프론트의 콜백 페이지에서 location.hash로 읽는다.
 */
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    private final JwtProvider jwtProvider;
    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Value("${app.frontend.oauth-redirect-path}")
    private String oauthRedirectPath;

    /**
     * 모바일 앱 복귀 리다이렉트 허용 스킴 목록(오픈 리다이렉트 방지). 여기 접두어로 시작하는 값만 허용한다.
     * naengkeum:// = 독립 실행/dev-build 앱 스킴, exp(s):// = Expo Go 개발 클라이언트.
     */
    @Value("${app.mobile.allowed-redirect-prefixes:naengkeum://,exp://,exps://}")
    private String allowedRedirectPrefixesRaw;

    public OAuth2SuccessHandler(JwtProvider jwtProvider,
                                HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository) {
        this.jwtProvider = jwtProvider;
        this.authorizationRequestRepository = authorizationRequestRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        // ⚠️ getAttribute(name)은 제네릭 <A> A 라서 String.valueOf(...)에 직접 넣으면
        //    반환 타입이 char[]로 잘못 추론된다. attributes Map(값 타입 Object)에서 꺼낸다.
        Map<String, Object> attributes = principal.getAttributes();
        Long memberId = Long.valueOf(String.valueOf(attributes.get("memberId")));
        String nickname = (String) attributes.get("nickname");
        String role = (String) attributes.get("role");

        // 소셜 로그인은 기본 만료(24h) 사용. rememberMe 개념 없음.
        String token = jwtProvider.createToken(memberId, nickname, role, false);

        // 모바일 복귀 리다이렉트(쿠키)는 쿠키 정리 전에 읽어둔다.
        String appRedirect = authorizationRequestRepository.readAppRedirect(request).orElse(null);

        // 인가요청 쿠키 정리
        authorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

        if (response.isCommitted()) {
            log.warn("응답이 이미 커밋됨 — 리다이렉트 생략 (memberId={})", memberId);
            return;
        }

        String targetUrl;
        if (appRedirect != null && isAllowedAppRedirect(appRedirect)) {
            // 모바일: 앱 스킴으로 복귀 (naengkeum://oauth#token=...). 토큰은 JWT(URL-safe)라 그대로 붙인다.
            targetUrl = appRedirect + (appRedirect.contains("#") ? "&" : "#") + "token=" + token;
            log.info("소셜 로그인 성공 — 모바일 앱으로 리다이렉트 (memberId={})", memberId);
        } else {
            if (appRedirect != null) {
                log.warn("허용되지 않은 app_redirect 무시 → 웹으로 리다이렉트 (memberId={})", memberId);
            }
            // 웹(기존 흐름): 프론트 콜백 페이지로 fragment 전달
            targetUrl = UriComponentsBuilder
                    .fromUriString(frontendBaseUrl + oauthRedirectPath)
                    .fragment("token=" + token)
                    .build(true)   // token은 JWT(URL-safe base64)라 추가 인코딩 불필요
                    .toUriString();
        }

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    /** app_redirect가 허용 스킴 접두어로 시작하는지 검증(오픈 리다이렉트 방지). */
    private boolean isAllowedAppRedirect(String appRedirect) {
        for (String prefix : allowedRedirectPrefixesRaw.split(",")) {
            String p = prefix.trim();
            if (!p.isEmpty() && appRedirect.startsWith(p)) {
                return true;
            }
        }
        return false;
    }
}
