package com.fridgefamer.mapper.follow;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 팔로우(follow) 액션 Mapper — Follow 그룹 (API 명세 §7, F16).
 *
 * <p>팔로우 <b>조회</b>(following/followers 목록)는 MemberMapper에 이미 있음.
 * 이 Mapper는 4주차에 추가되는 <b>액션</b>(등록/해제) + 보조 조회만 담당한다.</p>
 *
 * <p>follow 테이블 제약: uq_follow(follower_id, followee_id) 중복 차단(→409),
 * chk_follow_self(자기 자신 팔로우 차단). 자기 팔로우는 서비스에서 400으로 선제 차단한다.</p>
 */
@Mapper
public interface FollowMapper {

    /** 대상 회원이 존재하고 활성 상태인지(팔로우 대상 검증). */
    boolean memberExists(@Param("memberId") Long memberId);

    /** 이미 팔로우 중인지(중복 409 판단). */
    boolean exists(@Param("followerId") Long followerId,
                   @Param("followeeId") Long followeeId);

    /** 팔로우 등록. */
    int insertFollow(@Param("followerId") Long followerId,
                     @Param("followeeId") Long followeeId);

    /** 팔로우 해제. 삭제된 행 수(0이면 원래 팔로우 안 한 상태). */
    int deleteFollow(@Param("followerId") Long followerId,
                     @Param("followeeId") Long followeeId);

    /** 대상 유저의 현재 팔로워 수(토글 후 응답용). */
    int countFollowers(@Param("memberId") Long memberId);
}