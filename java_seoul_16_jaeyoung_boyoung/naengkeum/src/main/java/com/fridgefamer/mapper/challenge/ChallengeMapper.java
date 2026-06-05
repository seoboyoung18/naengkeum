package com.fridgefamer.mapper.challenge;

import com.fridgefamer.dto.response.challenge.ChallengeRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 챌린지(challenge) Mapper — Challenge 그룹 (API 명세 §8, F18).
 *
 * <p>SQL은 resources/mapper/challenge/ChallengeMapper.xml.
 * participantCount는 challenge_participant 집계, myProgress는 viewer(로그인 회원)의
 * 참여 행 LEFT JOIN으로 계산한다(비로그인/미참여면 null → 서비스에서 0).</p>
 *
 * <p>참여 중복은 uq_participant(challenge_id, member_id)로 차단(→409).</p>
 */
@Mapper
public interface ChallengeMapper {

    // ---- 조회 ----

    /**
     * 챌린지 목록. status=active면 진행 중(오늘 기간 내), ended면 종료된 것만.
     * viewerId nullable(비로그인 시 myProgress null).
     */
    List<ChallengeRow> selectList(@Param("viewerId") Long viewerId,
                                  @Param("status") String status);

    /** 회원이 참여 중인 챌린지 목록(/my). viewerId = 본인 id. */
    List<ChallengeRow> selectMy(@Param("viewerId") Long viewerId);

    /** 챌린지 단건 상세. 없으면 null. viewerId nullable. */
    ChallengeRow selectDetail(@Param("viewerId") Long viewerId,
                              @Param("challengeId") Long challengeId);

    /** 챌린지 존재 여부(참여 대상 검증). */
    boolean exists(@Param("challengeId") Long challengeId);

    // ---- 참여 ----

    /** 이미 참여 중인지(중복 409). */
    boolean isJoined(@Param("challengeId") Long challengeId,
                     @Param("memberId") Long memberId);

    /** 챌린지 참여 등록. */
    int insertParticipant(@Param("challengeId") Long challengeId,
                          @Param("memberId") Long memberId);

    /** 챌린지 참여 취소. 삭제 행 수(0이면 원래 미참여). */
    int deleteParticipant(@Param("challengeId") Long challengeId,
                          @Param("memberId") Long memberId);

    // ---- 통계 ----

    /** 진행 중 챌린지에 참여한 (중복 제거) 회원 수. */
    long countActiveParticipants();
}