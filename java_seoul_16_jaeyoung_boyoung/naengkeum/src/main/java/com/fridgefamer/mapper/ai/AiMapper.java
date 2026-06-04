package com.fridgefamer.mapper.ai;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * AI 추천 보조 조회 Mapper — 회원 알레르기 등 프롬프트 구성용.
 *
 * <p>냉장고 재료는 FridgeMapper를 재사용하므로 여기서는 알레르기만 조회한다.</p>
 */
@Mapper
public interface AiMapper {

    /** 회원 알레르기(콤마 구분 CSV). 없으면 null. */
    String selectAllergies(@Param("memberId") Long memberId);
}
