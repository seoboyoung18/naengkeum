package com.fridgefamer.dto.response.challenge;

import java.time.LocalDate;

/**
 * 챌린지 카드의 DB 조회 1행 — Mapper 전용 중간 DTO.
 *
 * <p>badge(중첩 객체)와 myStatus/myProgress(viewer 기준 계산)를 평면 컬럼으로 받는다.
 * 서비스에서 ChallengeItem으로 조립. dDay는 SQL에서 DATEDIFF로 계산.</p>
 */
public record ChallengeRow(
        Long challengeId,
        String title,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        long dDay,
        int participantCount,
        // 평면 badge 컬럼
        Long badgeId,
        String badgeName,
        String badgeIconUrl,
        // viewer 기준 (비로그인/미참여면 progress=null → 0 처리)
        Integer myProgress
) {}