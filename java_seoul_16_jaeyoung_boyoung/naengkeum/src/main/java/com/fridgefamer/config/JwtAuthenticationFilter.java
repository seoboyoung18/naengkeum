package com.fridgefamer.config;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 모든 HTTP 요청에서 JWT를 추출하여 SecurityContext에 인증 정보를 설정한다.
 *
 * <p>흐름:
 * <ol>
 *   <li>요청 헤더 Authorization에서 "Bearer xxx" 형태의 토큰 추출</li>
 *   <li>JwtProvider로 검증 → memberId 추출</li>
 *   <li>UsernamePasswordAuthenticationToken으로 SecurityContext에 저장</li>
 *   <li>이후 Controller에서 @AuthenticationPrincipal 또는 SecurityContextHolder로 사용</li>
 * </ol>
 *
 * <p>토큰이 없거나 잘못되면 인증 객체를 설정하지 않고 다음 필터로 넘긴다.
 * 인증이 필요한 엔드포인트면 Spring Security가 자동으로 401을 응답한다.
 * 단, 만료/위조 케이스는 디버깅을 위해 request attribute로 사유를 남긴다.</p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtProvider jwtProvider;

    @Value("${jwt.header}")
    private String headerName;

    @Value("${jwt.prefix}")
    private String tokenPrefix;

    public JwtAuthenticationFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null) {
            try {
                Long memberId = jwtProvider.getMemberId(token);
                String role = jwtProvider.getRole(token);   // USER / ADMIN

                // Spring Security 인증 객체 생성 (role → ROLE_ 권한)
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                memberId,                    // principal: memberId
                                null,                        // credentials: 불필요
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))  // ROLE_USER / ROLE_ADMIN
                        );
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (ExpiredJwtException e) {
                // 만료 — GlobalExceptionHandler가 TOKEN_EXPIRED로 응답할 수 있게 표시
                request.setAttribute("jwtErrorCode", "TOKEN_EXPIRED");
                log.debug("Expired JWT: {}", e.getMessage());

            } catch (JwtException | IllegalArgumentException e) {
                // 위조/형식 오류
                request.setAttribute("jwtErrorCode", "INVALID_TOKEN");
                log.debug("Invalid JWT: {}", e.getMessage());
            }
        }

        chain.doFilter(request, response);
    }

    /** "Authorization: Bearer xxx" 헤더에서 xxx 부분 추출. 없으면 null. */
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(headerName);
        if (bearer != null && bearer.startsWith(tokenPrefix)) {
            return bearer.substring(tokenPrefix.length());
        }
        return null;
    }
}