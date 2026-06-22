package com.fridgefamer.dto.request.seasoning;

import java.util.List;

/**
 * PUT /api/seasonings 요청 Body — 보유 조미료 "전체 집합"을 한 번에 저장.
 *
 * <p>모달의 "선택 완료" 시점에 선택된 seasoningId 목록을 통째로 보낸다.
 * 서버는 회원의 기존 보유를 모두 비우고 이 목록으로 재설정한다(set 동기화).
 * 비우려면 빈 배열(또는 null)을 보낸다. 존재하지 않는 id는 무시된다.</p>
 */
public record SeasoningOwnRequest(
        List<Long> seasoningIds
) {}
