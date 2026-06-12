package com.fridgefamer.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * SecurityContext에서 현재 인증 정보를 읽는 공통 유틸.
 *
 * <p>JwtAuthenticationFilter가 토큰의 role을 {@code ROLE_ADMIN}/{@code ROLE_USER}
 * 권한으로 세팅해 두므로, 여기서 그 권한을 조회해 관리자 여부를 판단한다.
 * 컨트롤러와 서비스(verifyOwner) 양쪽에서 재사용한다.</p>
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /** 현재 요청자가 관리자(ROLE_ADMIN)인지 여부. 미인증이면 false. */
    public static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}