package com.fridgefamer.admin;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 식약처 레시피 1건을 DB에 저장하는 영속화 컴포넌트 (WBS-④).
 *
 * <p>RecipeDataLoader와 분리한 이유: @Transactional은 Spring AOP 프록시로 동작하는데,
 * 같은 클래스 내 메서드 호출(self-invocation)은 프록시를 거치지 않아 트랜잭션이 적용되지 않는다.
 * 별도 빈으로 분리하여 saveOne() 호출 시 트랜잭션이 정상 작동하도록 한다.</p>
 *
 * <p>1건당 트랜잭션이므로, 특정 레시피의 재료/단계 파싱이 실패해도
 * 해당 레시피만 롤백되고 나머지는 정상 적재된다 (부분 실패 격리).</p>
 *
 * <p>⚠️ WBS-④ 1회성 적재용. 적재 완료 후 V5 SQL로 박제되면 이 코드는 제거 가능.</p>
 */
@Service
public class RecipePersister {

    private final JdbcTemplate jdbcTemplate;

    // 수량 분리용 정규식: "계란 2개", "물 200ml", "소금 약간" 등
    private static final Pattern QTY_PATTERN =
            Pattern.compile("^(.+?)\\s+([0-9/.]+\\s*[가-힣a-zA-Z]*|약간|적당량|조금|톡톡)$");

    // 헤더성 단어 (재료가 아닌 구분 라벨)
    private static final Pattern HEADER_PATTERN =
            Pattern.compile("^(재료|주재료|부재료|양념|양념장|소스|소스재료|곁들임)\\s*[:：]?\\s*$");

    public RecipePersister(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 레시피 1건 저장 (recipe + ingredients + steps).
     * REQUIRES_NEW: 각 레시피를 독립 트랜잭션으로 처리.
     */
    @Transactional
    public void saveOne(JsonNode row) {
        String title = row.path("RCP_NM").asText("").trim();
        if (title.isEmpty()) {
            throw new IllegalArgumentException("레시피명(RCP_NM)이 비어있음");
        }

        // ----- 1. recipe INSERT (generated key 확보) -----
        String category = row.path("RCP_PAT2").asText("");   // 요리 종류 (반찬/국&찌개 등)
        String way = row.path("RCP_WAY2").asText("");          // 조리 방법 (끓이기/볶기 등)
        String summary = buildSummary(category, way);
        String imageUrl = blankToNull(row.path("ATT_FILE_NO_MAIN").asText(""));

        // 영양정보 (식약처 COOKRCP01 제공, 완성 요리 1인분 기준)
        Integer calories = parseIntOrNull(row.path("INFO_ENG").asText(""));   // 열량(kcal)
        Double carbs = parseDoubleOrNull(row.path("INFO_CAR").asText(""));    // 탄수화물(g)
        Double protein = parseDoubleOrNull(row.path("INFO_PRO").asText(""));  // 단백질(g)
        Double fat = parseDoubleOrNull(row.path("INFO_FAT").asText(""));      // 지방(g)
        Double sodium = parseDoubleOrNull(row.path("INFO_NA").asText(""));    // 나트륨(mg)

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO recipe (title, summary, image_url, source, " +
                    "calories, carbs, protein, fat, sodium) " +
                    "VALUES (?, ?, ?, 'PUBLIC', ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, truncate(title, 100));
            ps.setString(2, summary);
            ps.setString(3, truncate(imageUrl, 500));
            // setObject: null이면 NULL로, 값 있으면 숫자로 (식약처 결측치 안전 처리)
            ps.setObject(4, calories);
            ps.setObject(5, carbs);
            ps.setObject(6, protein);
            ps.setObject(7, fat);
            ps.setObject(8, sodium);
            return ps;
        }, keyHolder);
        long recipeId = keyHolder.getKey().longValue();

        // ----- 2. 재료 파싱 + INSERT -----
        List<String[]> ingredients = parseIngredients(row.path("RCP_PARTS_DTLS").asText(""));
        for (String[] ing : ingredients) {
            jdbcTemplate.update(
                    "INSERT INTO recipe_ingredient (recipe_id, name, qty) VALUES (?, ?, ?)",
                    recipeId, truncate(ing[0], 50), truncate(ing[1], 50));
        }

        // ----- 3. 조리 단계 파싱 + INSERT (MANUAL01~20) -----
        int stepNumber = 1;
        for (int i = 1; i <= 20; i++) {
            String manual = row.path(String.format("MANUAL%02d", i)).asText("").trim();
            if (manual.isEmpty()) continue;

            // 앞의 "1.", "2 ." 같은 단계 번호 제거 (우리는 step_number로 따로 관리)
            String desc = manual.replaceFirst("^\\s*\\d+\\s*\\.?\\s*", "").trim();
            if (desc.isEmpty()) continue;

            String stepImg = blankToNull(row.path(String.format("MANUAL_IMG%02d", i)).asText(""));

            jdbcTemplate.update(
                    "INSERT INTO recipe_step (recipe_id, step_number, description, image_url) VALUES (?, ?, ?, ?)",
                    recipeId, stepNumber++, desc, truncate(stepImg, 500));
        }
    }

    /** 요약 = 카테고리 · 조리법 (예: "반찬 · 볶기") */
    private String buildSummary(String category, String way) {
        StringBuilder sb = new StringBuilder();
        if (!category.isEmpty()) sb.append(category);
        if (!way.isEmpty()) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(way);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /**
     * RCP_PARTS_DTLS 파싱. 식약처 데이터는 형식이 제각각이라 휴리스틱 적용:
     * - 줄바꿈/콤마로 항목 분리
     * - 앞 기호(●, •, -)와 헤더(재료:, 양념:) 제거
     * - "이름 + 수량" 패턴이면 분리, 아니면 전체를 이름으로
     *
     * @return [name, qty] 배열 리스트 (qty는 없으면 null)
     */
    private List<String[]> parseIngredients(String raw) {
        List<String[]> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) return result;

        String[] tokens = raw.split("[\\n,]");
        for (String token : tokens) {
            String item = token.trim().replaceAll("^[●•▶\\-\\s]+", "").trim();

            if (item.isEmpty() || HEADER_PATTERN.matcher(item).matches()) continue;

            // "재료: 쌀..." 같은 접두 라벨 제거
            item = item.replaceFirst(
                    "^(재료|주재료|부재료|양념|양념장|소스|소스재료|곁들임)\\s*[:：]\\s*", "").trim();
            if (item.isEmpty()) continue;

            String name = item;
            String qty = null;
            Matcher m = QTY_PATTERN.matcher(item);
            if (m.matches()) {
                name = m.group(1).trim();
                qty = m.group(2).trim();
            }
            if (name.isEmpty()) continue;
            result.add(new String[]{name, qty});
        }
        return result;
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    /**
     * 영양정보 정수 파싱. 식약처 데이터는 빈 값, "0", 비정상 문자가 섞여있어
     * 파싱 실패 시 null 반환 (적재 자체는 계속 진행).
     */
    private Integer parseIntOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            // "120.5" 같은 소수 표기도 정수로 (열량은 정수 컬럼)
            return (int) Math.round(Double.parseDouble(s.trim().replaceAll(",", "")));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 영양정보 실수 파싱 (탄단지나트륨, g/mg 단위). 실패 시 null. */
    private Double parseDoubleOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Double.parseDouble(s.trim().replaceAll(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}