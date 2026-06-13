package com.fridgefamer.controller;

import com.fridgefamer.dto.request.wishlist.SaveAiRecipeRequest;
import com.fridgefamer.dto.response.common.PageResponse;
import com.fridgefamer.dto.response.recipe.MyRecipeItem;
import com.fridgefamer.dto.response.recipe.RecipeAutocompleteItem;
import com.fridgefamer.dto.response.recipe.RecipeDetail;
import com.fridgefamer.dto.response.recipe.RecipeListItem;
import com.fridgefamer.dto.response.recipe.RecipePublished;
import com.fridgefamer.dto.response.recipe.RecipeSaved;
import com.fridgefamer.exception.ApiException;
import com.fridgefamer.exception.ErrorCode;
import com.fridgefamer.service.RecipeImageService;
import com.fridgefamer.service.RecipeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

/**
 * 레시피(Recipe) API — API 명세 §4.
 *
 * <ul>
 *   <li>GET /api/recipe              — F05 검색/필터/정렬 + 페이징</li>
 *   <li>GET /api/recipe/autocomplete — F05 제목 자동완성</li>
 *   <li>GET /api/recipe/popular      — F05 인기순 Top N</li>
 *   <li>GET /api/recipe/{recipeId}   — F05 상세</li>
 * </ul>
 *
 * <p>모두 공개(SecurityConfig permitAll "/api/recipe/**"). 인증 토큰이 있으면
 * isWishlisted가 채워지고, 없으면 false. /autocomplete·/popular는 정적 경로라
 * /{recipeId}보다 우선 매칭된다.</p>
 */
@RestController
@RequestMapping("/api/recipe")
@Validated
public class RecipeController {

    private final RecipeService recipeService;
    private final RecipeImageService recipeImageService;

    public RecipeController(RecipeService recipeService,
                            RecipeImageService recipeImageService) {
        this.recipeService = recipeService;
        this.recipeImageService = recipeImageService;
    }

    @GetMapping
    public PageResponse<RecipeListItem> search(
            @RequestParam(required = false) String keyword,

            @RequestParam(defaultValue = "LATEST")
            @Pattern(regexp = "LATEST|POPULAR|RATING|COOK_TIME",
                    message = "sort는 LATEST, POPULAR, RATING, COOK_TIME 중 하나여야 합니다")
            String sort,

            @RequestParam(required = false) String ingredients,   // 콤마 구분 재료명

            @RequestParam(required = false)
            @Positive(message = "minCookTime은 양수여야 합니다") Integer minCookTime,

            @RequestParam(required = false)
            @Positive(message = "maxCookTime은 양수여야 합니다") Integer maxCookTime,

            @RequestParam(defaultValue = "false") boolean mine,

            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다") int page,

            @RequestParam(defaultValue = "12") @Min(value = 1, message = "size는 1 이상이어야 합니다") int size
    ) {
        return recipeService.search(
                currentMemberIdOrNull(), trimToNull(keyword), parseCsv(ingredients),
                minCookTime, maxCookTime, sort, mine, page, size);
    }

    @GetMapping("/autocomplete")
    public List<RecipeAutocompleteItem> autocomplete(
            @RequestParam @NotBlank(message = "keyword는 비어 있을 수 없습니다") String keyword
    ) {
        return recipeService.autocomplete(keyword);
    }

    @GetMapping("/popular")
    public List<RecipeListItem> popular(
            @RequestParam(defaultValue = "8") @Min(value = 1, message = "limit은 1 이상이어야 합니다") int limit
    ) {
        return recipeService.popular(currentMemberIdOrNull(), limit);
    }

    @GetMapping("/{recipeId}")
    public RecipeDetail detail(
            @PathVariable @Positive(message = "recipeId는 양수여야 합니다") Long recipeId
    ) {
        return recipeService.detail(currentMemberIdOrNull(), recipeId);
    }

    // =================================================================
    //  쓰기 — AI 레시피 "담기" / 공개 / 마이 레시피 (인증 필요)
    // =================================================================

    /** "내 레시피로 담기"(콘텐츠) — D4 AI 결과를 마이 레시피(비공개)로 직접 등록. 찜과 무관. */
    @PostMapping("/from-ai")
    public ResponseEntity<RecipeSaved> registerFromAiContent(
            @Valid @RequestBody SaveAiRecipeRequest req
    ) {
        RecipeSaved saved = recipeService.createFromAiContent(currentMemberId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /** "내 레시피로 담기"(기존 ai_recipe id) — 이미 저장된 AI 레시피를 복사. 이미 담았으면 409. */
    @PostMapping("/from-ai/{aiRecipeId}")
    public ResponseEntity<RecipeSaved> registerFromAi(
            @PathVariable @Positive(message = "aiRecipeId는 양수여야 합니다") Long aiRecipeId
    ) {
        RecipeSaved saved = recipeService.createFromAi(currentMemberId(), aiRecipeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /** "공개하기" — 마이 레시피를 공개 카탈로그에 게시. 본인 소유만(403). */
    @PatchMapping("/{recipeId}/publish")
    public RecipePublished publish(
            @PathVariable @Positive(message = "recipeId는 양수여야 합니다") Long recipeId
    ) {
        return recipeService.publish(currentMemberId(), recipeId);
    }

    /**
     * 레시피 대표 사진 업로드 — 본인이 등록한 레시피에만.
     * multipart/form-data, 파트명 "image". 반환: 저장된 이미지 공개 URL.
     */
    @PostMapping("/{recipeId}/image")
    public Map<String, String> uploadImage(
            @PathVariable @Positive(message = "recipeId는 양수여야 합니다") Long recipeId,
            @RequestParam("image") MultipartFile image
    ) {
        String url = recipeImageService.upload(currentMemberId(), recipeId, image);
        return Map.of("imageUrl", url);
    }

    /** 마이 레시피 목록 — author_id=나 (공개/비공개 포함). */
    @GetMapping("/mine")
    public List<MyRecipeItem> mine() {
        return recipeService.listMine(currentMemberId());
    }

    // =================================================================
    //  내부 헬퍼
    // =================================================================

    /** 콤마 구분 문자열 → 트림·공백제거된 재료명 리스트. null/빈값이면 빈 리스트. */
    private List<String> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** 인증 선택. 익명/미인증이면 null → isWishlisted=false. */
    private Long currentMemberIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        return (principal instanceof Long id) ? id : null;
    }

    /** 인증 필수(담기/공개/마이). 미인증이면 401. (SecurityConfig에서도 막지만 이중 방어) */
    private Long currentMemberId() {
        Long id = currentMemberIdOrNull();
        if (id == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "인증이 필요합니다");
        }
        return id;
    }
}