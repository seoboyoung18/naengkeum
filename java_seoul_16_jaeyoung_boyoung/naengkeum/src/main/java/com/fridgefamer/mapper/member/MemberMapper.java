package com.fridgefamer.mapper.member;

import com.fridgefamer.dto.response.auth.MemberAuthRow;
import com.fridgefamer.dto.response.member.BadgeItem;
import com.fridgefamer.dto.response.member.FollowUserItem;
import com.fridgefamer.dto.response.member.MemberBasicRow;
import com.fridgefamer.dto.response.member.MyPageResponse.MyStats;
import com.fridgefamer.dto.response.member.MyReviewItem;
import com.fridgefamer.dto.response.member.OtherProfileResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 회원(member) 테이블 Mapper — Auth + Member 그룹 사용.
 *
 * <p>SQL은 src/main/resources/mapper/member/MemberMapper.xml 에 정의.
 * Auth 그룹: countByEmail/countByNickname/insertMember/findAuthByEmail.
 * Member 그룹: findById, count*, update*, softDelete, selectMyReviews+count,
 *              selectFollowing/Followers, selectBadges, selectOtherProfile.</p>
 */
@Mapper
public interface MemberMapper {

    // =================================================================
    //  Auth 그룹
    // =================================================================

    int countByEmail(@Param("email") String email);

    int countByNickname(@Param("nickname") String nickname);

    int insertMember(@Param("cmd") RegisterCommand cmd);

    MemberAuthRow findAuthByEmail(@Param("email") String email);

    // =================================================================
    //  Member 그룹 — 기본 조회 / 수정
    // =================================================================

    /** 활성 회원 단건 조회. 탈퇴(is_active=0) 회원은 null. */
    MemberBasicRow findActiveById(@Param("memberId") Long memberId);

    /** 닉네임 중복(자기 자신 제외). */
    int countByNicknameExcept(@Param("nickname") String nickname,
                              @Param("memberId") Long memberId);

    int updateNickname(@Param("memberId") Long memberId,
                       @Param("nickname") String nickname);

    int updatePassword(@Param("memberId") Long memberId,
                       @Param("password") String passwordHash);

    int updateAllergies(@Param("memberId") Long memberId,
                        @Param("allergies") String allergiesCsv);

    /** 소프트 삭제 — is_active=0 으로 전환. */
    int softDeleteById(@Param("memberId") Long memberId);

    // =================================================================
    //  Member 그룹 — 통계 / 카운트
    // =================================================================

    int countFridgeByMemberId(@Param("memberId") Long memberId);

    int countReviewByMemberId(@Param("memberId") Long memberId);

    int countWishlistByMemberId(@Param("memberId") Long memberId);

    int countFollowerByMemberId(@Param("memberId") Long memberId);

    int countFollowingByMemberId(@Param("memberId") Long memberId);

    /** 통계 5종 한 방 조회(서브쿼리). */
    MyStats selectMyStats(@Param("memberId") Long memberId);

    // =================================================================
    //  Member 그룹 — 목록
    // =================================================================

    List<MyReviewItem> selectMyReviews(@Param("memberId") Long memberId,
                                       @Param("offset") int offset,
                                       @Param("size") int size);

    /** /me/following — 본인이 팔로우하는 유저들. isFollowing은 항상 true. */
    List<FollowUserItem> selectFollowing(@Param("memberId") Long memberId);

    /**
     * /me/followers — 본인을 팔로우하는 유저들.
     * isFollowing은 "본인이 그 유저를 맞팔로우 중인지" — 서브쿼리로 계산.
     */
    List<FollowUserItem> selectFollowers(@Param("memberId") Long memberId);

    List<BadgeItem> selectBadges(@Param("memberId") Long memberId);

    /**
     * /{userId}/profile — 단건 프로필.
     * viewerId는 호출자(로그인 회원) id. 비로그인 시 null → isFollowing은 false로 SQL 처리.
     */
    OtherProfileResponse selectOtherProfile(@Param("memberId") Long memberId,
                                            @Param("viewerId") Long viewerId);

    // =================================================================
    //  내부 인자 클래스 — useGeneratedKeys 회수용 (record는 immutable이라 setter X)
    // =================================================================
    class RegisterCommand {
        private Long memberId;
        private final String email;
        private final String password;
        private final String nickname;
        private final String allergies;
        private final boolean marketingAgree;

        public RegisterCommand(String email, String password, String nickname,
                               String allergies, boolean marketingAgree) {
            this.email = email;
            this.password = password;
            this.nickname = nickname;
            this.allergies = allergies;
            this.marketingAgree = marketingAgree;
        }

        public Long getMemberId()          { return memberId; }
        public void setMemberId(Long id)   { this.memberId = id; }
        public String getEmail()           { return email; }
        public String getPassword()        { return password; }
        public String getNickname()        { return nickname; }
        public String getAllergies()       { return allergies; }
        public boolean isMarketingAgree()  { return marketingAgree; }
    }
}
