package com.fridgefamer.service;

import com.fridgefamer.config.oauth.OAuthAttributes;
import com.fridgefamer.dto.response.auth.MemberAuthRow;
import com.fridgefamer.mapper.member.MemberMapper;
import com.fridgefamer.mapper.member.MemberMapper.SocialRegisterCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 소셜 로그인 회원 조회/가입 로직.
 *
 * <p>식별 순서:
 * <ol>
 *   <li>(provider, socialId)로 기존 소셜 회원 조회 → 있으면 그대로 로그인</li>
 *   <li>이메일이 있으면 같은 이메일의 기존 회원에 소셜 연결 후 로그인
 *       (팀 정책: "같은 이메일이면 연결")</li>
 *   <li>둘 다 없으면 신규 가입</li>
 * </ol>
 * 탈퇴(is_active=0) 회원이 매칭되면 로그인 거부(OAuth2AuthenticationException).
 */
@Service
public class OAuthMemberService {

    private static final Logger log = LoggerFactory.getLogger(OAuthMemberService.class);

    private final MemberMapper memberMapper;

    public OAuthMemberService(MemberMapper memberMapper) {
        this.memberMapper = memberMapper;
    }

    /** 인증된 소셜 사용자 정보로 우리 회원을 확정한다. */
    @Transactional
    public ResolvedMember resolve(OAuthAttributes attrs) {
        // 1) 소셜 식별자로 기존 회원 조회
        MemberAuthRow bySocial = memberMapper.findBySocial(attrs.provider(), attrs.socialId());
        if (bySocial != null) {
            ensureActive(bySocial);
            return ResolvedMember.from(bySocial);
        }

        // 2) 같은 이메일의 기존 회원에 연결
        if (attrs.email() != null && !attrs.email().isBlank()) {
            MemberAuthRow byEmail = memberMapper.findAuthByEmail(attrs.email());
            if (byEmail != null) {
                ensureActive(byEmail);
                memberMapper.linkSocial(byEmail.memberId(), attrs.provider(), attrs.socialId());
                log.info("Linked social to existing member: id={}, provider={}",
                        byEmail.memberId(), attrs.provider());
                return ResolvedMember.from(byEmail);
            }
        }

        // 3) 신규 가입
        String nickname = generateUniqueNickname(attrs);
        SocialRegisterCommand cmd =
                new SocialRegisterCommand(attrs.email(), nickname, attrs.provider(), attrs.socialId());
        memberMapper.insertSocialMember(cmd);
        log.info("Registered social member: id={}, provider={}, nickname={}",
                cmd.getMemberId(), attrs.provider(), nickname);
        return new ResolvedMember(cmd.getMemberId(), nickname, "USER");
    }

    private void ensureActive(MemberAuthRow row) {
        if (!row.isActive()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("member_withdrawn"), "탈퇴한 회원입니다");
        }
    }

    /**
     * 닉네임은 UNIQUE 제약이 있어 충돌 시 숫자 접미사를 붙여 유일하게 만든다.
     * 닉네임 컬럼은 VARCHAR(20)이므로 접미사 자리를 남기고 베이스를 16자로 자른다.
     */
    private String generateUniqueNickname(OAuthAttributes attrs) {
        String base = attrs.nickname();
        if (base == null || base.isBlank()) {
            base = ("GOOGLE".equals(attrs.provider()) ? "구글" : "카카오") + "유저";
        }
        base = base.trim();
        if (base.length() > 16) {
            base = base.substring(0, 16);
        }

        String candidate = base;
        int suffix = 0;
        while (memberMapper.countByNickname(candidate) > 0) {
            suffix++;
            candidate = base + suffix;
        }
        return candidate;
    }

    /** 토큰 발급에 필요한 최소 회원 정보. */
    public record ResolvedMember(Long memberId, String nickname, String role) {
        static ResolvedMember from(MemberAuthRow row) {
            return new ResolvedMember(row.memberId(), row.nickname(), row.role());
        }
    }
}
