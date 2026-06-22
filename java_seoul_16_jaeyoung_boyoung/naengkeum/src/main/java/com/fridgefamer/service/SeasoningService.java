package com.fridgefamer.service;

import com.fridgefamer.dto.response.seasoning.SeasoningItem;
import com.fridgefamer.mapper.seasoning.SeasoningMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 조미료(Seasoning) 도메인 서비스.
 *
 * <p>조미료는 수량/유통기한 없이 "보유 여부"만 관리한다. 저장은 모달의 "선택 완료"에서
 * 보유 집합 전체를 받아 기존 보유를 비우고 재설정하는 set 동기화 방식이다.</p>
 */
@Service
public class SeasoningService {

    private final SeasoningMapper seasoningMapper;

    public SeasoningService(SeasoningMapper seasoningMapper) {
        this.seasoningMapper = seasoningMapper;
    }

    /** 조미료 카탈로그 + 내 보유여부. */
    public List<SeasoningItem> list(Long memberId) {
        return seasoningMapper.selectCatalogWithOwned(memberId);
    }

    /**
     * 보유 조미료 집합 저장(전량 삭제 후 재삽입). 갱신된 카탈로그를 반환한다.
     * null/중복/null요소는 정리하고, 빈 집합이면 모두 비운다.
     */
    @Transactional
    public List<SeasoningItem> saveOwned(Long memberId, List<Long> seasoningIds) {
        List<Long> ids = (seasoningIds == null) ? List.of()
                : seasoningIds.stream().filter(Objects::nonNull).distinct().toList();

        seasoningMapper.deleteOwnedByMember(memberId);
        if (!ids.isEmpty()) {
            seasoningMapper.insertOwned(memberId, ids);
        }
        return seasoningMapper.selectCatalogWithOwned(memberId);
    }
}
