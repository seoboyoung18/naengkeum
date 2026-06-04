package com.fridgefamer.mapper.ingredient;

import com.fridgefamer.dto.response.ingredient.IngredientAutocompleteItem;
import com.fridgefamer.dto.response.ingredient.IngredientSuggestResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 식재료 사전(ingredient_dictionary) 테이블 Mapper — Ingredient 그룹.
 *
 * <p>SQL은 src/main/resources/mapper/ingredient/IngredientMapper.xml 에 정의.
 * 읽기 전용(자동완성·보관기한 제안)이라 조회 메서드만 둔다.</p>
 */
@Mapper
public interface IngredientMapper {

    /**
     * 식재료명 부분일치 자동완성. 접두 일치를 우선 노출하고 이름순 정렬.
     * @param keyword 검색어(부분일치, 트림된 값)
     * @param limit   최대 반환 건수
     */
    List<IngredientAutocompleteItem> selectAutocomplete(@Param("keyword") String keyword,
                                                        @Param("limit") int limit);

    /**
     * 식재료명 정확일치 보관 정보 조회. 없으면 null(서비스에서 폴백 응답 생성).
     * found 컬럼은 SQL에서 상수 TRUE로 채워 매핑한다.
     */
    IngredientSuggestResponse selectByName(@Param("name") String name);
}
