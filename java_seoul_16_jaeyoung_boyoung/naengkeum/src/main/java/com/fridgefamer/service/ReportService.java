package com.fridgefamer.service;

import com.fridgefamer.dto.request.report.CreateReportRequest;
import com.fridgefamer.exception.ApiException;
import com.fridgefamer.exception.ErrorCode;
import com.fridgefamer.mapper.recipe.RecipeMapper;
import com.fridgefamer.mapper.report.ReportMapper;
import com.fridgefamer.mapper.review.ReviewMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 신고(Report) 도메인 서비스.
 *
 * <p>대상(레시피/리뷰) 존재 여부를 기존 매퍼로 선검증해 없으면 404.
 * 같은 회원의 중복 신고는 DB UNIQUE → DuplicateKeyException → 409로 자동 변환된다.</p>
 */
@Service
public class ReportService {

    private final ReportMapper reportMapper;
    private final RecipeMapper recipeMapper;
    private final ReviewMapper reviewMapper;

    public ReportService(ReportMapper reportMapper,
                         RecipeMapper recipeMapper,
                         ReviewMapper reviewMapper) {
        this.reportMapper = reportMapper;
        this.recipeMapper = recipeMapper;
        this.reviewMapper = reviewMapper;
    }

    /**
     * 콘텐츠 신고 생성. targetType에 따라 recipe_id 또는 review_id에 기록한다.
     * 대상이 없으면 404, 같은 회원이 이미 신고했으면 409(DuplicateKey).
     */
    @Transactional
    public void create(Long reporterId, CreateReportRequest req) {
        String reason = trimToNull(req.reason());
        if ("RECIPE".equals(req.targetType())) {
            if (recipeMapper.selectRecipeOwner(req.targetId()) == null) {
                throw new ApiException(ErrorCode.NOT_FOUND, "레시피를 찾을 수 없습니다");
            }
            reportMapper.insertReport(reporterId, req.targetId(), null, reason);
        } else { // REVIEW (DTO @Pattern이 RECIPE|REVIEW만 허용)
            if (reviewMapper.findOwnerId(req.targetId()) == null) {
                throw new ApiException(ErrorCode.NOT_FOUND, "리뷰를 찾을 수 없습니다");
            }
            reportMapper.insertReport(reporterId, null, req.targetId(), reason);
        }
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
