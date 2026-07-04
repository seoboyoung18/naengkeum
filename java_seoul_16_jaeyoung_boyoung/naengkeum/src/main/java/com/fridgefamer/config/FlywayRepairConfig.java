package com.fridgefamer.config;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway 기동 전략 — 마이그레이션 전에 repair()를 먼저 수행한다(모든 환경).
 *
 * <p><b>왜 필요한가</b></p>
 * <ol>
 *   <li><b>체크섬 드리프트</b>: 환경마다(집·싸피·운영) flyway_schema_history가 서로 다른
 *       시점/Flyway 버전으로 기록돼, 동일 마이그레이션인데도 "checksum mismatch"로 기동이 막힌다.
 *       repair()는 데이터/스키마를 건드리지 않고 히스토리의 체크섬을 현재 파일 기준으로 재정렬한다.</li>
 *   <li><b>실패 레코드 정리</b>: 과거에 중단된 마이그레이션의 실패(success=0) 레코드를 정리해
 *       재적용이 가능하게 한다.</li>
 * </ol>
 *
 * <p>이후 migrate()로 미적용 마이그레이션(V14·V15 등)을 적용한다. repair는 멱등이라 매 기동마다
 * 안전하게 반복 가능하다.</p>
 *
 * <p><b>주의(트레이드오프)</b>: 모든 환경에서 repair가 돌면, 누군가 <b>이미 적용된</b> 마이그레이션
 * 파일을 의도치 않게 수정해도 체크섬 불일치로 잡아내지 못하고 그대로 받아들인다. 본 프로젝트는
 * (a) 팀이 작고 마이그레이션을 의도적으로 관리하며, (b) Flyway/부트 버전 상향으로 환경 간
 * 체크섬 드리프트가 이미 발생해 운영 재배포에도 repair가 필요하므로 이 방식을 채택한다.
 * 마이그레이션은 "한 번 적용되면 수정 금지(새 버전 추가)" 원칙을 반드시 지킬 것.</p>
 */
@Configuration
public class FlywayRepairConfig {

    @Bean
    public FlywayMigrationStrategy repairThenMigrate() {
        return flyway -> {
            flyway.repair();   // 체크섬 재정렬 + 실패 레코드 정리(데이터 불변)
            flyway.migrate();  // 미적용 마이그레이션 적용
        };
    }
}
