package com.fridgefamer.mapper.report;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 신고(report) Mapper — Report 그룹.
 *
 * <p>SQL은 src/main/resources/mapper/report/ReportMapper.xml.
 * recipe_id / review_id 중 하나만 채워 INSERT한다(나머지는 null → DB XOR CHECK 통과).
 * 같은 회원의 중복 신고는 UNIQUE 위반 → DuplicateKeyException → 409로 자동 변환.</p>
 */
@Mapper
public interface ReportMapper {

    /**
     * 신고 1건 추가. recipeId/reviewId 중 정확히 하나만 non-null이어야 한다(나머지 null).
     * 중복 신고 시 DuplicateKey → 409.
     */
    int insertReport(@Param("reporterId") Long reporterId,
                     @Param("recipeId") Long recipeId,
                     @Param("reviewId") Long reviewId,
                     @Param("reason") String reason);
}
