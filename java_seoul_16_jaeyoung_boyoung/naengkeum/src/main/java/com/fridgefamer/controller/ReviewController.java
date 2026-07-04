package com.fridgefamer.controller;

import com.fridgefamer.dto.request.review.CreateReviewRequest;
import com.fridgefamer.dto.request.review.UpdateReviewRequest;
import com.fridgefamer.dto.response.review.ReviewItem;
import com.fridgefamer.dto.response.review.ReviewListResponse;
import com.fridgefamer.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 리뷰(Review) API — API 명세 §5.
 *
 * <ul>
 *   <li>GET    /api/review?recipeId= — F07 목록 조회(+ratingStats), 공개</li>
 *   <li>POST   /api/review           — F06 작성(201), 인증, 중복 409</li>
 *   <li>PUT    /api/review/{id}      — F08 수정, 인증, 본인만 403</li>
 *   <li>DELETE /api/review/{id}      — F09 삭제, 인증, 403/404</li>
 * </ul>
 *
 * <p>GET만 공개(SecurityConfig GET "/api/review" permitAll). POST/PUT/DELETE는 인증 필요.</p>
 */
@RestController
@RequestMapping("/api/review")
@Validated
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ReviewListResponse list(
            @RequestParam @NotNull(message = "recipeId는 필수입니다")
            @Positive(message = "recipeId는 양수여야 합니다") Long recipeId,

            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size는 1 이상이어야 합니다") int size
    ) {
        return reviewService.list(recipeId, currentMemberIdOrNull(), page, size);
    }

    @PostMapping
    public ResponseEntity<ReviewItem> create(@Valid @RequestBody CreateReviewRequest req) {
        ReviewItem created = reviewService.create(currentMemberId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{reviewId}")
    public ReviewItem update(
            @PathVariable @Positive(message = "reviewId는 양수여야 합니다") Long reviewId,
            @Valid @RequestBody UpdateReviewRequest req
    ) {
        return reviewService.update(currentMemberId(), reviewId, req);
    }

    @DeleteMapping("/{reviewId}")
    public Map<String, String> delete(
            @PathVariable @Positive(message = "reviewId는 양수여야 합니다") Long reviewId
    ) {
        reviewService.delete(currentMemberId(), reviewId);
        return Map.of("message", "삭제 완료");
    }

    // =================================================================
    //  내부 헬퍼 — SecurityContext에서 memberId 추출
    // =================================================================

    /** 인증 필수(작성/수정/삭제). */
    private Long currentMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }

    /** 인증 선택(목록의 isOwner 계산). 익명/미인증이면 null. */
    private Long currentMemberIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        return (principal instanceof Long id) ? id : null;
    }
}
