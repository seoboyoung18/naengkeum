package com.fridgefamer.config.oauth;

import java.util.Map;

/**
 * 제공자(구글/카카오)마다 다른 사용자 정보 응답을 공통 형태로 정규화한다.
 *
 * <p>구글: 평탄한 구조 — {@code { sub, email, name, picture, ... }}
 * <p>카카오: 중첩 구조 — {@code { id, kakao_account: { email, profile: { nickname } } } }
 */
public record OAuthAttributes(
        String provider,   // GOOGLE / KAKAO
        String socialId,   // 제공자 고유 사용자 ID
        String email,      // 없을 수 있음(null)
        String nickname    // 없을 수 있음(null)
) {

    public static OAuthAttributes of(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> ofGoogle(attributes);
            case "kakao" -> ofKakao(attributes);
            default -> throw new IllegalArgumentException("지원하지 않는 소셜 제공자: " + registrationId);
        };
    }

    private static OAuthAttributes ofGoogle(Map<String, Object> attr) {
        return new OAuthAttributes(
                "GOOGLE",
                asString(attr.get("sub")),
                asString(attr.get("email")),
                asString(attr.get("name"))
        );
    }

    private static OAuthAttributes ofKakao(Map<String, Object> attr) {
        String socialId = asString(attr.get("id"));   // 카카오 회원번호(Long) → 문자열

        String email = null;
        String nickname = null;

        Object accountObj = attr.get("kakao_account");
        if (accountObj instanceof Map<?, ?> account) {
            email = asString(account.get("email"));
            Object profileObj = account.get("profile");
            if (profileObj instanceof Map<?, ?> profile) {
                nickname = asString(profile.get("nickname"));
            }
        }
        // 일부 앱 설정에선 properties.nickname 으로도 내려옴 — 폴백.
        if (nickname == null) {
            Object propsObj = attr.get("properties");
            if (propsObj instanceof Map<?, ?> props) {
                nickname = asString(props.get("nickname"));
            }
        }
        return new OAuthAttributes("KAKAO", socialId, email, nickname);
    }

    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
