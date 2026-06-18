package com.fridgefamer.config.oauth;

import com.fridgefamer.service.OAuthMemberService;
import com.fridgefamer.service.OAuthMemberService.ResolvedMember;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 제공자에서 사용자 정보를 가져온 뒤, 우리 회원으로 변환(조회/가입)하여
 * principal로 사용할 OAuth2User를 만든다.
 *
 * <p>반환하는 OAuth2User의 attributes에는 우리 회원 식별값(memberId/nickname/role)을
 * 담는다. {@code OAuth2SuccessHandler}가 이 값으로 우리 JWT를 발급한다.
 * nameAttributeKey는 "memberId"로 두어 getName()이 회원 PK를 돌려주게 한다.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final OAuthMemberService oAuthMemberService;

    public CustomOAuth2UserService(OAuthMemberService oAuthMemberService) {
        this.oAuthMemberService = oAuthMemberService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 제공자 user-info 엔드포인트 호출 (구글/카카오)
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuthAttributes attrs = OAuthAttributes.of(registrationId, oAuth2User.getAttributes());

        ResolvedMember member = oAuthMemberService.resolve(attrs);

        Map<String, Object> principalAttributes = new HashMap<>();
        principalAttributes.put("memberId", member.memberId());
        principalAttributes.put("nickname", member.nickname());
        principalAttributes.put("role", member.role());

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_" + member.role())),
                principalAttributes,
                "memberId");
    }
}
