package com.fridgefamer.service;

import com.fridgefamer.dto.response.challenge.ChallengeBadge;
import com.fridgefamer.dto.response.challenge.ChallengeItem;
import com.fridgefamer.dto.response.challenge.ChallengeJoinResponse;
import com.fridgefamer.dto.response.challenge.ChallengeProgressResponse;
import com.fridgefamer.dto.response.challenge.ChallengeRow;
import com.fridgefamer.dto.response.challenge.ChallengeStatsResponse;
import com.fridgefamer.exception.ApiException;
import com.fridgefamer.exception.ErrorCode;
import com.fridgefamer.mapper.challenge.ChallengeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 챌린지(Challenge) 도메인 서비스 — API 명세 §8 (F18).
 *
 * <p>목록/상세/통계는 공개(nullable viewerId), 내챌린지/참여/언조인은 인증 필요.
 * ChallengeRow(평면)를 ChallengeItem(badge 중첩 + myStatus)로 조립한다.</p>
 *
 * <p>myProgress가 null이면 미참여(myStatus=NONE, progress=0), 값이 있으면
 * 참여 중(myStatus=JOINED).</p>
 */
@Service
public class ChallengeService {

    private static final String STATUS_JOINED = "JOINED";
    private static final String STATUS_NONE = "NONE";

    private final ChallengeMapper challengeMapper;

    public ChallengeService(ChallengeMapper challengeMapper) {
        this.challengeMapper = challengeMapper;
    }

    // =====================================================================
    //  GET /api/challenge — 목록 (공개)
    // =====================================================================
    public List<ChallengeItem> list(Long viewerId, String status) {
        return challengeMapper.selectList(viewerId, status).stream()
                .map(this::toItem)
                .toList();
    }

    // =====================================================================
    //  GET /api/challenge/my — 내가 참여 중인 챌린지 (인증)
    // =====================================================================
    public List<ChallengeItem> myChallenges(Long memberId) {
        return challengeMapper.selectMy(memberId).stream()
                .map(this::toItem)
                .toList();
    }

    // =====================================================================
    //  GET /api/challenge/{challengeId} — 상세 (공개)
    // =====================================================================
    public ChallengeItem detail(Long viewerId, Long challengeId) {
        ChallengeRow row = challengeMapper.selectDetail(viewerId, challengeId);
        if (row == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "챌린지를 찾을 수 없습니다");
        }
        return toItem(row);
    }

    // =====================================================================
    //  POST /api/challenge/{challengeId}/join — 참여 (인증)
    // =====================================================================
    @Transactional
    public ChallengeJoinResponse join(Long memberId, Long challengeId) {
        if (!challengeMapper.exists(challengeId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "챌린지를 찾을 수 없습니다");
        }
        if (challengeMapper.isJoined(challengeId, memberId)) {
            throw new ApiException(ErrorCode.CONFLICT, "이미 참여 중인 챌린지입니다");
        }
        challengeMapper.insertParticipant(challengeId, memberId);
        return new ChallengeJoinResponse(true);
    }

    // =====================================================================
    //  DELETE /api/challenge/{challengeId}/join — 언조인 (인증)
    // =====================================================================
    @Transactional
    public ChallengeJoinResponse unjoin(Long memberId, Long challengeId) {
        // 멱등 처리: 미참여 상태에서 취소해도 결과는 "미참여"로 동일
        challengeMapper.deleteParticipant(challengeId, memberId);
        return new ChallengeJoinResponse(false);
    }

    // =====================================================================
    //  PATCH /api/challenge/{challengeId}/progress — 진행률 갱신 + 배지 자동지급 (인증)
    // =====================================================================
    @Transactional
    public ChallengeProgressResponse updateProgress(Long memberId, Long challengeId, int progress) {
        // 참여 중이어야 진행률 갱신 가능. 미참여면 updateProgress가 0행 → 404.
        int updated = challengeMapper.updateProgress(challengeId, memberId, progress);
        if (updated == 0) {
            throw new ApiException(ErrorCode.NOT_FOUND, "참여 중인 챌린지가 아닙니다");
        }

        boolean achieved = progress >= 100;
        boolean badgeEarned = false;

        // 100% 달성 시 챌린지 보상 배지를 지급(중복은 INSERT IGNORE로 무시).
        if (achieved) {
            Long badgeId = challengeMapper.selectBadgeId(challengeId);
            if (badgeId != null) {
                int inserted = challengeMapper.insertMemberBadge(memberId, badgeId);
                badgeEarned = (inserted == 1); // 1=신규 지급, 0=이미 보유
            }
        }

        return new ChallengeProgressResponse(progress, achieved, badgeEarned);
    }

    // =====================================================================
    //  GET /api/challenge/stats — 활성 사용자 통계 (공개)
    // =====================================================================
    public ChallengeStatsResponse stats() {
        return new ChallengeStatsResponse(challengeMapper.countActiveParticipants());
    }

    // =====================================================================
    //  내부 헬퍼 — Row → Item 조립
    // =====================================================================
    private ChallengeItem toItem(ChallengeRow row) {
        ChallengeBadge badge = new ChallengeBadge(
                row.badgeId(), row.badgeName(), row.badgeIconUrl());

        boolean joined = row.myProgress() != null;
        String myStatus = joined ? STATUS_JOINED : STATUS_NONE;
        int myProgress = joined ? row.myProgress() : 0;

        return new ChallengeItem(
                row.challengeId(),
                row.title(),
                row.description(),
                row.startDate(),
                row.endDate(),
                row.dDay(),
                row.participantCount(),
                badge,
                myStatus,
                myProgress
        );
    }
}