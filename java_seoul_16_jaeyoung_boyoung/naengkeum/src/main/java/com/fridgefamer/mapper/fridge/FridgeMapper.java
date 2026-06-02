package com.fridgefamer.mapper.fridge;

import com.fridgefamer.dto.response.fridge.FridgeItem;
import com.fridgefamer.dto.response.fridge.FridgeSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 냉장고(fridge_item) 테이블 Mapper — Fridge 그룹.
 *
 * <p>SQL은 src/main/resources/mapper/fridge/FridgeMapper.xml 에 정의.
 * dDay는 DATEDIFF(expiry_date, CURDATE())로 SQL에서 계산해 내려준다.
 * 정렬(sort)·보관위치(storageType) 필터는 SQL injection 방어를 위해
 * XML의 &lt;choose&gt;/&lt;if&gt;로 안전하게 분기한다(문자열 연결 금지).</p>
 */
@Mapper
public interface FridgeMapper {

    // ---- 조회 ----

    /**
     * 회원 냉장고 목록.
     * @param storageType FRIDGE|FREEZER|ROOM_TEMP|ALL ("ALL"이면 전체)
     * @param sort        EXPIRY_ASC|CREATED_DESC|NAME_ASC
     */
    List<FridgeItem> selectByMember(@Param("memberId") Long memberId,
                                    @Param("storageType") String storageType,
                                    @Param("sort") String sort);

    /** 보관 위치별 개수 요약(한 방 조회). */
    FridgeSummary selectSummary(@Param("memberId") Long memberId);

    /** dDay &lt;= days 인 임박 재료를 임박순으로. */
    List<FridgeItem> selectExpiring(@Param("memberId") Long memberId,
                                    @Param("days") int days);

    /** 단건 조회(dDay 포함). 응답 생성용. 없으면 null. */
    FridgeItem selectItemById(@Param("fridgeItemId") Long fridgeItemId);

    /** 소유자 회원 id만 조회 — 권한 검증용. 없으면 null. */
    Long findOwnerId(@Param("fridgeItemId") Long fridgeItemId);

    /** 레시피 재료 중 회원이 보유(name 일치)한 recipe_ingredient.ingredient_id 목록. */
    List<Long> selectOwnedIngredientIds(@Param("memberId") Long memberId,
                                        @Param("recipeId") Long recipeId);

    // ---- 변경 ----

    int insertFridgeItem(@Param("cmd") FridgeItemCommand cmd);

    int updateFridgeItem(@Param("fridgeItemId") Long fridgeItemId,
                         @Param("cmd") FridgeItemCommand cmd);

    int deleteById(@Param("fridgeItemId") Long fridgeItemId);

    // =================================================================
    //  내부 인자 클래스 — useGeneratedKeys 회수용 (record는 immutable이라 setter X)
    // =================================================================
    class FridgeItemCommand {
        private Long fridgeItemId;
        private final Long memberId;
        private final String name;
        private final BigDecimal qty;
        private final String unit;
        private final String storageType;
        private final LocalDate expiryDate;
        private final String memo;

        public FridgeItemCommand(Long memberId, String name, BigDecimal qty, String unit,
                                 String storageType, LocalDate expiryDate, String memo) {
            this.memberId = memberId;
            this.name = name;
            this.qty = qty;
            this.unit = unit;
            this.storageType = storageType;
            this.expiryDate = expiryDate;
            this.memo = memo;
        }

        public Long getFridgeItemId()        { return fridgeItemId; }
        public void setFridgeItemId(Long id) { this.fridgeItemId = id; }
        public Long getMemberId()            { return memberId; }
        public String getName()              { return name; }
        public BigDecimal getQty()           { return qty; }
        public String getUnit()              { return unit; }
        public String getStorageType()       { return storageType; }
        public LocalDate getExpiryDate()     { return expiryDate; }
        public String getMemo()              { return memo; }
    }
}
