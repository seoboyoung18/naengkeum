package com.fridgefamer.controller;

import com.fridgefamer.dto.response.ingredient.IngredientAutocompleteItem;
import com.fridgefamer.dto.response.ingredient.IngredientSuggestResponse;
import com.fridgefamer.service.IngredientService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 식재료 사전(Ingredient) API — API 명세 §12.
 *
 * <ul>
 *   <li>GET /api/ingredients/autocomplete — F21 식재료명 부분일치 자동완성</li>
 *   <li>GET /api/ingredients/suggest      — F22 보관기한/보관법 제안(미존재 시 직접입력 폴백)</li>
 * </ul>
 *
 * <p>두 엔드포인트 모두 인증 불필요(SecurityConfig permitAll "/api/ingredients/**").</p>
 */
@RestController
@RequestMapping("/api/ingredients")
@Validated   // @RequestParam(@NotBlank 등) 검증 트리거
public class IngredientController {

    private final IngredientService ingredientService;

    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @GetMapping("/autocomplete")
    public List<IngredientAutocompleteItem> autocomplete(
            @RequestParam @NotBlank(message = "keyword는 비어 있을 수 없습니다") String keyword
    ) {
        return ingredientService.autocomplete(keyword);
    }

    @GetMapping("/suggest")
    public IngredientSuggestResponse suggest(
            @RequestParam @NotBlank(message = "name은 비어 있을 수 없습니다") String name
    ) {
        return ingredientService.suggest(name);
    }
}
