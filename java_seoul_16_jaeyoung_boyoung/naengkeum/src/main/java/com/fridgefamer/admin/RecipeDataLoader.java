package com.fridgefamer.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 식약처 조리식품 레시피 DB(COOKRCP01) 적재 오케스트레이터 (WBS-④).
 *
 * <p>API 호출 → JSON 파싱 → RecipePersister에 1건씩 위임.
 * 트랜잭션은 RecipePersister.saveOne()에 걸려 있어 1건당 격리된다.</p>
 *
 * <p>식약처 API 형식:
 * GET https://openapi.foodsafetykorea.go.kr/api/{KEY}/COOKRCP01/json/{start}/{end}
 * 응답: { "COOKRCP01": { "total_count": "...", "row": [ {...}, ... ] } }</p>
 *
 * <p>⚠️ WBS-④ 1회성 적재용. 적재 후 V5 SQL로 박제되면 제거 가능.</p>
 */
@Component
public class RecipeDataLoader {

    private static final Logger log = LoggerFactory.getLogger(RecipeDataLoader.class);

    /** 식약처 API는 큰 구간에서 일부 누락되므로 50건씩 안정적으로 페이징 */
    private static final int BATCH_SIZE = 50;

    private final JdbcTemplate jdbcTemplate;
    private final RecipePersister persister;
    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${foodsafety.api-key}")
    private String apiKey;

    @Value("${foodsafety.base-url}")
    private String baseUrl;

    public RecipeDataLoader(JdbcTemplate jdbcTemplate, RecipePersister persister) {
        this.jdbcTemplate = jdbcTemplate;
        this.persister = persister;
    }

    /**
     * count개의 레시피를 적재.
     *
     * @param count         적재할 레시피 수
     * @param clearExisting true면 기존 PUBLIC 레시피 전체 삭제 후 적재 (멱등성 확보)
     */
    public LoadResult load(int count, boolean clearExisting) {
        if (clearExisting) {
            // CASCADE 설정으로 recipe 삭제 시 ingredient/step도 함께 삭제됨
            int deleted = jdbcTemplate.update("DELETE FROM recipe WHERE source = 'PUBLIC'");
            log.info("[적재] 기존 PUBLIC 레시피 {}건 삭제", deleted);
        }

        int loaded = 0;
        int failed = 0;

        for (int start = 1; start <= count; start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE - 1, count);
            log.info("[적재] API 호출: {} ~ {}", start, end);

            JsonNode rows;
            try {
                rows = fetchRecipes(start, end);
            } catch (Exception e) {
                log.error("[적재] API 호출 실패 ({}~{}): {}", start, end, e.getMessage());
                break;  // API 자체 실패면 중단
            }

            if (rows == null || !rows.isArray() || rows.isEmpty()) {
              log.warn("[적재] 빈 배치 (start={}), 다음 배치 계속", start);
              continue;   // break → continue (중간 빈 구간 건너뛰고 계속)
            }

            for (JsonNode row : rows) {
                String name = row.path("RCP_NM").asText("(이름없음)");
                try {
                    persister.saveOne(row);   // 별도 빈 호출 → 트랜잭션 적용
                    loaded++;
                } catch (Exception e) {
                    failed++;
                    log.warn("[적재] 실패: {} - {}", name, e.getMessage());
                }
            }
        }

        log.info("[적재] 완료 — 성공 {}건, 실패 {}건", loaded, failed);
        return new LoadResult(loaded, failed);
    }

    /** 식약처 API 호출 → COOKRCP01.row 배열 반환 */
    private JsonNode fetchRecipes(int start, int end) throws Exception {
        String url = String.format("%s/%s/COOKRCP01/json/%d/%d", baseUrl, apiKey, start, end);

        String body = restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(body);

        // API 에러 응답 체크 (인증키 오류 등)
        JsonNode result = root.path("COOKRCP01").path("RESULT");
        String code = result.path("CODE").asText("");
        if (!code.isEmpty() && !"INFO-000".equals(code)) {
            throw new IllegalStateException(
                    "식약처 API 오류: " + code + " - " + result.path("MSG").asText());
        }

        return root.path("COOKRCP01").path("row");
    }

    /** 적재 결과 요약 */
    public record LoadResult(int loaded, int failed) {}
}