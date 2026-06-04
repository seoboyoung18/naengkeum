package com.fridgefamer.controller;

import com.fridgefamer.dto.response.common.PageResponse;
import com.fridgefamer.dto.response.recipe.RecipeAutocompleteItem;
import com.fridgefamer.dto.response.recipe.RecipeCard;
import com.fridgefamer.dto.response.recipe.RecipeDetail;
import com.fridgefamer.service.RecipeService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * 레시피(Recipe) API — API 명세 §4.
 *
 * <ul>
 *   <li>GET /api/recipe              — F05 검색 (keyword·sort·ingredients·maxCookTime·page·size)</li>
 *   <li>GET /api/recipe/autocomplete — F05 자동완성 (최대 5개)</li>
 *   <li>GET /api/recipe/popular      — F05 인기 레시피</li>
 *   <li>GET /api/recipe/{recipeId}   — F05 상세</li>
 * </ul>
 *
 * <p>전부 인증 불필요(SecurityConfig에서 /api/recipe/** permitAll).
 * 단, 로그인 상태면 찜 여부(isWishlisted)를 채워주기 위해 nullable memberId를 추출한다.</p>
 */
@RestController
@RequestMapping("/api/recipe")
@Validated
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping
    public PageResponse<RecipeCard> search(
            @RequestParam(required = false) String keyword,

            @RequestParam(defaultValue = "LATEST")
            @Pattern(regexp = "LATEST|POPULAR|RATING|COOK_TIME",
                    message = "sort는 LATEST, POPULAR, RATING, COOK_TIME 중 하나여야 합니다")
            String sort,

            @RequestParam(required = false) String ingredients,

            @RequestParam(required = false)
            @Positive(message = "maxCookTime은 양수여야 합니다")
            Integer maxCookTime,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page는 0 이상이어야 합니다")
            int page,

            @RequestParam(defaultValue = "12")
            @Min(value = 1, message = "size는 1 이상이어야 합니다")
            @Max(value = 100, message = "size는 100 이하여야 합니다")
            int size
    ) {
        List<String> ingredientList = parseIngredients(ingredients);
        return recipeService.search(
                currentMemberIdOrNull(), keyword, sort, ingredientList, maxCookTime, page, size);
    }

    @GetMapping("/autocomplete")
    public List<RecipeAutocompleteItem> autocomplete(
            @RequestParam String keyword
    ) {
        return recipeService.autocomplete(keyword);
    }

    @GetMapping("/popular")
    public List<RecipeCard> popular(
            @RequestParam(defaultValue = "8")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다")
            @Max(value = 50, message = "limit은 50 이하여야 합니다")
            int limit
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
    //  내부 헬퍼
    // =================================================================

    /** 콤마 구분 재료 문자열 → 리스트(공백 제거, 빈 값 제외). null이면 null. */
    private List<String> parseIngredients(String ingredients) {
        if (ingredients == null || ingredients.isBlank()) {
            return null;
        }
        return Arrays.stream(ingredients.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * 공개 API용 memberId 추출. 비로그인이면 null.
     * <p>FridgeController.currentMemberId()는 인증 필수라 principal을 무조건 Long으로
     * 캐스팅하지만, 공개 API는 익명 사용자(principal이 "anonymousUser" String)일 수 있어
     * instanceof로 안전하게 분기한다.</p>
     */
    private Long currentMemberIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        Object principal = auth.getPrincipal();
        return (principal instanceof Long memberId) ? memberId : null;
    }
}