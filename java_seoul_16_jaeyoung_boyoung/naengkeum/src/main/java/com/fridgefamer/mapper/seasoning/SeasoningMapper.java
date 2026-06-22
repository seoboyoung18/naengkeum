package com.fridgefamer.mapper.seasoning;

import com.fridgefamer.dto.response.seasoning.SeasoningItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 조미료(seasoning / member_seasoning) 테이블 Mapper.
 *
 * <p>SQL은 src/main/resources/mapper/seasoning/SeasoningMapper.xml 에 정의.
 * owned는 member_seasoning 존재 여부로 SQL에서 계산해 내려준다.
 * 보유 저장은 "전량 삭제 후 재삽입(set 동기화)"이므로 delete/insert 두 메서드를 쓴다.</p>
 */
@Mapper
public interface SeasoningMapper {

    /** 조미료 마스터 전체 + 회원 보유 여부(owned). sort_order, name 순. */
    List<SeasoningItem> selectCatalogWithOwned(@Param("memberId") Long memberId);

    /** 회원의 보유 조미료 전체 삭제(set 동기화 1단계). */
    int deleteOwnedByMember(@Param("memberId") Long memberId);

    /**
     * 회원 보유 조미료 일괄 삽입(set 동기화 2단계).
     * 존재하지 않는 seasoning_id는 SELECT 필터로 무시한다(잘못된 id에 500 방지).
     * @param ids 비어있지 않은 seasoningId 목록(서비스에서 빈 리스트 가드).
     */
    int insertOwned(@Param("memberId") Long memberId, @Param("ids") List<Long> ids);
}
