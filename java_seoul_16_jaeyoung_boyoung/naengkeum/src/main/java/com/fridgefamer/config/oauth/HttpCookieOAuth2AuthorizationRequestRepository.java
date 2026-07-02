package com.fridgefamer.config.oauth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * OAuth2 인가요청(state/nonce 등)을 HTTP 세션 대신 <b>쿠키</b>에 저장한다.
 *
 * <p>이 앱은 SecurityConfig가 SessionCreationPolicy.STATELESS라 세션 기반
 * 기본 저장소({@code HttpSessionOAuth2AuthorizationRequestRepository})를 쓰면
 * 콜백 시점에 인가요청을 못 찾아({@code authorization_request_not_found}) 실패한다.
 * 쿠키 저장 방식은 무상태를 유지하고, 다중 인스턴스(로드밸런서) 배포에도 안전하다.
 *
 * <p>쿠키는 직렬화 후 Base64(URL-safe)로 인코딩. httpOnly + 짧은 만료(3분)로
 * 노출/탈취 위험을 줄인다. 콜백 처리 후 즉시 삭제된다.
 */
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String OAUTH2_REQUEST_COOKIE = "oauth2_auth_request";
    /**
     * 모바일 앱이 인가요청을 시작할 때 함께 넘기는 복귀용 앱 스킴 리다이렉트(app_redirect 파라미터)를
     * 담는 쿠키. 값은 Base64(URL-safe)로 인코딩한다(스킴 URL의 ':' '/' 가 쿠키 값 제약을 어기지 않도록).
     * 웹 로그인은 이 파라미터를 보내지 않으므로 쿠키가 없고 → 기존 웹 리다이렉트 흐름 그대로.
     */
    public static final String APP_REDIRECT_COOKIE = "oauth2_app_redirect";
    /** 모바일 앱이 전달하는 복귀 리다이렉트 파라미터명. */
    public static final String APP_REDIRECT_PARAM = "app_redirect";
    private static final int COOKIE_MAX_AGE_SECONDS = 180;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return findCookie(request)
                .map(cookie -> deserialize(cookie.getValue()))
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequestCookies(request, response);
            return;
        }
        boolean secure = request.isSecure();
        writeCookie(response, OAUTH2_REQUEST_COOKIE, serialize(authorizationRequest), COOKIE_MAX_AGE_SECONDS, secure);

        // 모바일 흐름이면 복귀용 앱 리다이렉트를 쿠키로 실어 콜백까지 이어준다(웹은 파라미터 없음 → 미저장).
        String appRedirect = request.getParameter(APP_REDIRECT_PARAM);
        if (StringUtils.hasText(appRedirect)) {
            String encoded = Base64.getUrlEncoder().encodeToString(appRedirect.getBytes(StandardCharsets.UTF_8));
            writeCookie(response, APP_REDIRECT_COOKIE, encoded, COOKIE_MAX_AGE_SECONDS, secure);
        }
    }

    /** 성공 핸들러용: 저장해 둔 앱 복귀 리다이렉트(app_redirect)를 복원한다. 없으면 비어있음(=웹 흐름). */
    public Optional<String> readAppRedirect(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie c : cookies) {
            if (APP_REDIRECT_COOKIE.equals(c.getName()) && StringUtils.hasText(c.getValue())) {
                try {
                    return Optional.of(new String(Base64.getUrlDecoder().decode(c.getValue()), StandardCharsets.UTF_8));
                } catch (Exception e) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                 HttpServletResponse response) {
        // Spring Security 6: remove 시 쿠키 삭제까지 책임진다.
        OAuth2AuthorizationRequest authRequest = loadAuthorizationRequest(request);
        removeAuthorizationRequestCookies(request, response);
        return authRequest;
    }

    /** 성공/실패 핸들러에서 인가요청·앱리다이렉트 쿠키를 제거할 때 호출. */
    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        boolean secure = request.isSecure();
        writeCookie(response, OAUTH2_REQUEST_COOKIE, "", 0, secure);
        writeCookie(response, APP_REDIRECT_COOKIE, "", 0, secure);
    }

    // ----- 내부 헬퍼 -----

    private java.util.Optional<Cookie> findCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return java.util.Optional.empty();
        }
        for (Cookie c : cookies) {
            if (OAUTH2_REQUEST_COOKIE.equals(c.getName())) {
                return java.util.Optional.of(c);
            }
        }
        return java.util.Optional.empty();
    }

    private void writeCookie(HttpServletResponse response, String name, String value, int maxAge, boolean secure) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        cookie.setSecure(secure);
        response.addCookie(cookie);
    }

    private static String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(authorizationRequest);
            oos.flush();
            return Base64.getUrlEncoder().encodeToString(bos.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("OAuth2 인가요청 직렬화 실패", e);
        }
    }

    private static OAuth2AuthorizationRequest deserialize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        byte[] bytes = Base64.getUrlDecoder().decode(value);
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (OAuth2AuthorizationRequest) ois.readObject();
        } catch (Exception e) {
            // 손상/위조된 쿠키는 조용히 무시 → 인가요청 없음으로 처리
            return null;
        }
    }
}
