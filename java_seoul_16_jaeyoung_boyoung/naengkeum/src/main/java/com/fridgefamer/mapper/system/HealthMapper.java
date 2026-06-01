package com.fridgefamer.mapper.system;

import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

/**
 * 시스템 헬스체크용 Mapper (WBS-②-5).
 *
 * <p>MyBatis 동작 검증용 임시 Mapper. 2주차에 도메인별 Mapper
 * (AuthMapper, MemberMapper 등)가 만들어지면 패턴 참고용으로 유지.</p>
 *
 * <p>SQL은 src/main/resources/mapper/system/HealthMapper.xml에 정의.
 * 인터페이스 메서드명과 XML statement id가 1:1 매칭되어야 함.</p>
 *
 * <p>@Mapper 어노테이션은 @MapperScan을 쓰므로 생략 가능하지만,
 * IDE 가독성을 위해 명시 권장.</p>
 */
@Mapper
public interface HealthMapper {

    /** 회원 수 */
    int countMembers();

    /** 냉장고 재료 수 (전체) */
    int countFridgeItems();

    /** 첫 번째 회원 정보 (snake_case → camelCase 자동 매핑 검증) */
    Map<String, Object> findFirstMember();

    /** 임박 재료 수 (D-3 이내) — 2주차 dashboard 쿼리 미리보기 */
    int countExpiringSoon();
}