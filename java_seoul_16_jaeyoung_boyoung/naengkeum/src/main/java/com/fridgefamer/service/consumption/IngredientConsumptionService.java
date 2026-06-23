package com.fridgefamer.service.consumption;

import com.fridgefamer.dto.response.fridge.FridgeItem;
import com.fridgefamer.dto.response.recipe.RecipeIngredient;
import com.fridgefamer.mapper.fridge.FridgeMapper;
import com.fridgefamer.mapper.recipe.RecipeMapper;
import com.fridgefamer.mapper.seasoning.SeasoningMapper;
import com.fridgefamer.service.consumption.ConsumptionPlan.Deduction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 레시피 공개 시 냉장고 재고 자동 차감 — 계산({@link ConsumptionPlanner})과 DB 반영을 잇는 서비스.
 *
 * <p>{@link RecipeService#publish}가 "최초 공개 1회"에만 호출한다(재공개 중복 차감은
 * recipe.ingredients_consumed 플래그로 호출 측에서 가드). 같은 트랜잭션에 참여하므로
 * 공개 전환·차감·플래그 셋이 원자적으로 처리된다.</p>
 */
@Service
public class IngredientConsumptionService {

    private final RecipeMapper recipeMapper;
    private final FridgeMapper fridgeMapper;
    private final SeasoningMapper seasoningMapper;

    public IngredientConsumptionService(RecipeMapper recipeMapper,
                                        FridgeMapper fridgeMapper,
                                        SeasoningMapper seasoningMapper) {
        this.recipeMapper = recipeMapper;
        this.fridgeMapper = fridgeMapper;
        this.seasoningMapper = seasoningMapper;
    }

    /**
     * 레시피의 정량·이름일치·단위호환 재료를 냉장고에서 차감하고(0이면 항목 삭제),
     * ingredients_consumed=TRUE로 고정한 뒤 차감 계획을 반환한다.
     *
     * @param memberId 공개하는 회원(=냉장고 소유자)
     * @param recipeId 공개되는 레시피
     * @return 차감/스킵 내역(응답 토스트·로그용)
     */
    @Transactional
    public ConsumptionPlan applyOnPublish(Long memberId, Long recipeId) {
        List<RecipeIngredient> ingredients = recipeMapper.selectIngredients(recipeId);
        // 임박순으로 받아, 이름이 같은 항목이 여럿이면 유통기한이 가까운 것부터 차감.
        List<FridgeItem> fridge = fridgeMapper.selectByMember(memberId, "ALL", "EXPIRY_ASC");
        Set<String> seasonings = new HashSet<>(seasoningMapper.selectAllCatalogNames());

        ConsumptionPlan plan = ConsumptionPlanner.plan(ingredients, fridge, seasonings);

        for (Deduction d : plan.deductions()) {
            if (d.removeItem()) {
                fridgeMapper.deleteById(d.fridgeItemId());   // 다 써서 0 → 냉장고에서 삭제
            } else {
                fridgeMapper.updateQty(d.fridgeItemId(), d.after());
            }
        }
        recipeMapper.markIngredientsConsumed(recipeId);
        return plan;
    }
}
