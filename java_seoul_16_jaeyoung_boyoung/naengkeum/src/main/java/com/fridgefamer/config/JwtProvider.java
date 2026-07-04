package com.fridgefamer.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 생성/검증 컴포넌트.
 *
 * <p>알고리즘: HS256 (HMAC + SHA-256)
 * 비밀키는 application.yml의 jwt.secret에서 주입.</p>
 *
 * <p>토큰 페이로드(Claims):
 * <ul>
 *   <li>sub  - memberId (Long을 String으로)</li>
 *   <li>nickname - 화면 표시용</li>
 *   <li>iat / exp - 자동 부여</li>
 * </ul>
 * 비밀번호, 이메일 등 민감정보는 절대 포함하지 않는다.</p>
 *
 * <p>WBS-②-3에서 작성. 2주차 AuthService에서 사용 예정.</p>
 */
@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.long-expiration}")
    private long longExpiration;

    /** application.yml 주입 후 SecretKey 객체로 변환 (1회만) */
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        // HS256은 최소 256bit (32byte) 키 필요. 짧으면 예외 발생.
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 토큰 발급.
     *
     * @param memberId   회원 PK
     * @param nickname   화면 표시용 닉네임
     * @param rememberMe true면 30일, false면 24시간
     */
    public String createToken(Long memberId, String nickname, String role, boolean rememberMe) {
        long ttl = rememberMe ? longExpiration : expiration;
        Date now = new Date();
        Date exp = new Date(now.getTime() + ttl);

        return Jwts.builder()
                .subject(String.valueOf(memberId))      // sub
                .claim("nickname", nickname)
                .claim("role", role)                    // USER / ADMIN
                .issuedAt(now)
                .expiration(exp)
                .signWith(secretKey)                    // HS256 자동 선택
                .compact();
    }

    /**
     * 토큰에서 회원 ID 추출.
     * 검증 실패 시 JwtException 계열의 예외가 던져진다.
     *
     * @throws ExpiredJwtException 만료된 토큰
     * @throws JwtException        서명 불일치 등 위조된 토큰
     */
    public Long getMemberId(String token) {
        Claims claims = parseClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    public String getNickname(String token) {
        return parseClaims(token).get("nickname", String.class);
    }

    /** 토큰에서 역할(role)을 추출한다. 옛 토큰(role claim 없음)은 USER로 간주. */
    public String getRole(String token) {
        String role = parseClaims(token).get("role", String.class);
        return role != null ? role : "USER";
    }

    /**
     * 토큰 유효성 검사 + Claims 추출.
     * 호출자가 JwtException을 catch하여 ErrorCode로 변환한다.
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}