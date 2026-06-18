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

        // 인가요청 쿠키 정리
        authorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

        if (response.isCommitted()) {
            log.warn("응답이 이미 커밋됨 — 리다이렉트 생략 (memberId={})", memberId);
            return;
        }

        String targetUrl = UriComponentsBuilder
                .fromUriString(frontendBaseUrl + oauthRedirectPath)
                .fragment("token=" + token)
                .build(true)   // token은 JWT(URL-safe base64)라 추가 인코딩 불필요
                .toUriString();

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
