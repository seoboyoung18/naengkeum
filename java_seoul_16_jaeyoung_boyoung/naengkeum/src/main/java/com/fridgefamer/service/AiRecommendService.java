package com.fridgefamer.service;

import com.fridgefamer.dto.request.ai.AiRecommendRequest;
import com.fridgefamer.dto.response.fridge.FridgeItem;
import com.fridgefamer.dto.response.recipe.RecipeIngredient;
import com.fridgefamer.dto.response.recipe.RecipeMatchCandidate;
import com.fridgefamer.dto.response.recipe.RecipeStep;
import com.fridgefamer.exception.ApiException;
import com.fridgefamer.exception.ErrorCode;
import com.fridgefamer.mapper.ai.AiMapper;
import com.fridgefamer.mapper.fridge.FridgeMapper;
import com.fridgefamer.mapper.recipe.RecipeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import tools.jackson.databind.json.JsonMapper;

/**
 * AI 레시피 추천 서비스 — API 명세 §9 F19. POST /api/ai/recommend (SSE).
 *
 * <p>냉장고 재료 + 알레르기로 프롬프트를 만들어 OpenAI Chat Completions(JSON 모드)를 호출하고,
 * 결과를 SSE 이벤트(title→summary→ingredient*→step*→meta→done)로 흘려보낸다.
 * 빈 냉장고는 연결 전 400, 키 미설정은 연결 전 503으로 차단하고, 호출 도중 실패는
 * {@code data:{"type":"error","retryable":true}} 이벤트로 알린다.</p>
 *
 * <p>ObjectMapper 빈에 의존하지 않도록(프로젝트 방침) LLM 응답 파싱은 로컬 JsonMapper로 처리한다.</p>
 */
@Service
public class AiRecommendService {

    private static final Logger log = LoggerFactory.getLogger(AiRecommendService.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final long SSE_TIMEOUT_MS = 120_000L;
    private static final int NEAR_EXPIRY_DAYS = 7;

    /** DB 레시피를 바로 추천하기 위한 최소 보유 재료 수(이 이상 겹쳐야 DB 후보). */
    private static final int MIN_MATCH_FOR_DB = 2;
    /** DB 레시피 추천 허용 조건: 실제로 사야 하는 재료(보유분·기본양념 제외)가 이 개수 이하일 때만. */
    private static final int MAX_INGREDIENTS_TO_BUY = 4;
    /** 매칭 후보 조회 개수(상위군에서 무작위로 골라 다양성 확보). */
    private static final int MATCH_CANDIDATE_LIMIT = 12;
    /** 이 수 이상 DB 후보가 있으면 항상 DB에서 추천. 미만이면 다양성을 위해 일부 확률로 AI도 섞는다. */
    private static final int ENOUGH_DB_CANDIDATES = 3;

    /**
     * "사야 할 재료" 계산에서 제외하는 기본양념/상비품 — 사실상 누구나 갖고 있다고 본다.
     * (안 빼면 소금·간장 때문에 간단한 요리도 '사야 할 게 많다'고 잘못 걸러짐)
     */
    private static final List<String> STAPLES = List.of(
            "소금", "설탕", "후추", "후춧가루", "식용유", "올리브유", "참기름", "들기름",
            "간장", "진간장", "국간장", "양조간장", "된장", "고추장", "고춧가루", "식초",
            "맛술", "미림", "물", "다진마늘", "다진 마늘", "마늘", "생강", "깨", "통깨",
            "참깨", "깨소금", "굴소스", "마요네즈", "케첩", "케찹", "물엿", "올리고당",
            "전분", "녹말", "밀가루"
    );

    /** 같은 재료로도 다양한 요리가 나오도록 매 호출 무작위로 고르는 조리 스타일 힌트. */
    private static final String[] STYLE_HINTS = {
            "볶음 또는 볶음밥류", "국·탕·찌개류", "면 요리(국수/파스타/볶음면 등)",
            "구이·부침·전류", "덮밥·비빔밥류", "간단한 반찬·안주류",
            "오븐/에어프라이어 요리", "죽·리조또류", "샐러드·무침류", "찜·조림류"
    };

    private final FridgeMapper fridgeMapper;
    private final AiMapper aiMapper;
    private final RecipeMapper recipeMapper;
    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public AiRecommendService(FridgeMapper fridgeMapper,
                              AiMapper aiMapper,
                              RecipeMapper recipeMapper,
                              @Value("${openai.base-url}") String baseUrl,
                              @Value("${openai.api-key}") String apiKey,
                              @Value("${openai.model}") String model) {
        this.fridgeMapper = fridgeMapper;
        this.aiMapper = aiMapper;
        this.recipeMapper = recipeMapper;
        this.apiKey = apiKey;
        this.model = model;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    // =====================================================================
    //  POST /api/ai/recommend — 동기 사전검증 후 SSE 비동기 스트리밍 시작
    // =====================================================================
    public SseEmitter recommend(Long memberId, AiRecommendRequest req) {
        // 1) 연결 전 차단 — 빈 냉장고(400), 키 미설정(503)
        List<FridgeItem> fridge = fridgeMapper.selectByMember(memberId, "ALL", "EXPIRY_ASC");
        if (fridge.isEmpty()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "냉장고에 재료가 없습니다. 먼저 재료를 등록해 주세요.");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiException(ErrorCode.AI_SERVICE_ERROR, "AI 서비스가 설정되지 않았습니다.");
        }
        String allergies = req.applyAllergyOrDefault() ? aiMapper.selectAllergies(memberId) : null;

        // 2) SSE 시작 — 이후 실패는 error 이벤트로 전달
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        executor.submit(() -> stream(emitter, fridge, allergies, req));
        return emitter;
    }

    // =====================================================================
    //  비동기 스트리밍 본체
    // =====================================================================
    private void stream(SseEmitter emitter, List<FridgeItem> fridge, String allergies, AiRecommendRequest req) {
        try {
            Set<String> owned = fridge.stream()
                    .map(f -> f.name().trim().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());

            // ===== 1단계: 냉장고로 "현실적으로 만들 수 있는" DB 레시피 매칭 =====
            //  (보유 ≥ MIN_MATCH_FOR_DB 그리고 사야 할 재료 ≤ MAX_INGREDIENTS_TO_BUY)
            List<String> names = fridge.stream().map(FridgeItem::name).toList();
            List<RecipeMatchCandidate> candidates = recipeMapper.selectFridgeMatches(
                    names, STAPLES, MIN_MATCH_FOR_DB, MAX_INGREDIENTS_TO_BUY, MATCH_CANDIDATE_LIMIT);

            // 후보가 넉넉하면 항상 DB, 적으면(1~2개) 절반 확률로만 DB → 나머지는 AI로 다양성.
            boolean useDb = !candidates.isEmpty()
                    && (candidates.size() >= ENOUGH_DB_CANDIDATES
                        || ThreadLocalRandom.current().nextBoolean());

            if (useDb) {
                // 후보는 모두 '현실적(사야 할 재료 ≤ MAX_INGREDIENTS_TO_BUY)'이므로 그 안에서 무작위로.
                RecipeMatchCandidate chosen =
                        candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
                streamDbRecipe(emitter, chosen, owned);
                return;
            }

            // ===== 2단계: AI 생성 (현실적 DB 후보가 없거나, 적어서 다양성을 위해 AI 선택) =====
            //  후보가 있으면 참고로 전달해 AI가 현실성을 유지하도록 한다.
            streamAiRecipe(emitter, fridge, owned, allergies, req, candidates);
        } catch (Exception e) {
            log.warn("AI recommend stream failed: {}", e.getMessage());
            sendErrorQuietly(emitter, e.getMessage());
            emitter.complete();
        }
    }

    // =====================================================================
    //  DB 레시피 스트리밍 (source=DB)
    // =====================================================================
    private void streamDbRecipe(SseEmitter emitter, RecipeMatchCandidate chosen, Set<String> owned) throws Exception {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("origin", "DB");
        source.put("recipeId", chosen.recipeId());
        send(emitter, "source", source);

        send(emitter, "title", chosen.title());
        if (chosen.summary() != null && !chosen.summary().isBlank()) {
            send(emitter, "summary", chosen.summary());
        }
        for (RecipeIngredient ing : recipeMapper.selectIngredients(chosen.recipeId())) {
            boolean isOwned = ing.name() != null
                    && owned.contains(ing.name().trim().toLowerCase(Locale.ROOT));
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("name", ing.name());
            value.put("qty", ing.qty());
            value.put("unit", null);
            value.put("owned", isOwned);
            send(emitter, "ingredient", value);
        }
        for (RecipeStep step : recipeMapper.selectSteps(chosen.recipeId())) {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("stepNumber", step.stepNumber());
            value.put("description", step.description());
            send(emitter, "step", value);
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("cookTime", chosen.cookTime());
        send(emitter, "meta", meta);
        send(emitter, "done", null);
        emitter.complete();
    }

    // =====================================================================
    //  AI 생성 스트리밍 (source=AI) — 약한 DB 후보를 참고로 전달
    // =====================================================================
    private void streamAiRecipe(SseEmitter emitter, List<FridgeItem> fridge, Set<String> owned,
                                String allergies, AiRecommendRequest req,
                                List<RecipeMatchCandidate> references) throws Exception {
        send(emitter, "source", Map.of("origin", "AI"));

        Map<String, Object> recipe = callOpenAi(fridge, allergies, req, references);

        send(emitter, "title", recipe.getOrDefault("title", "추천 레시피"));
            if (recipe.get("summary") != null) {
                send(emitter, "summary", recipe.get("summary"));
            }
            for (Object ing : asList(recipe.get("ingredients"))) {
                Map<String, Object> m = asMap(ing);
                Object name = m.get("name");
                boolean isOwned = name != null
                        && owned.contains(name.toString().trim().toLowerCase(Locale.ROOT));
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("name", name);
                value.put("qty", m.get("qty"));
                value.put("unit", m.get("unit"));
                value.put("owned", isOwned);
                send(emitter, "ingredient", value);
            }
            for (Object step : asList(recipe.get("steps"))) {
                send(emitter, "step", asMap(step));
            }
            if (recipe.get("meta") != null) {
                send(emitter, "meta", recipe.get("meta"));
            }
            send(emitter, "done", null);
            emitter.complete();
    }

    // =====================================================================
    //  OpenAI Chat Completions (JSON 모드) 호출 → 파싱된 레시피 Map
    // =====================================================================
    @SuppressWarnings("unchecked")
    private Map<String, Object> callOpenAi(List<FridgeItem> fridge, String allergies,
                                           AiRecommendRequest req, List<RecipeMatchCandidate> references) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", 1.0);   // 다양성↑ (같은 재료 반복 호출 시 비슷한 결과 방지)
        body.put("top_p", 0.95);
        body.put("response_format", Map.of("type", "json_object"));
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt()),
                Map.of("role", "user", "content", userPrompt(fridge, allergies, req, references))));

        Map<String, Object> resp = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        List<Object> choices = asList(resp == null ? null : resp.get("choices"));
        if (choices.isEmpty()) {
            throw new IllegalStateException("LLM 응답에 choices가 없습니다");
        }
        Map<String, Object> message = asMap(asMap(choices.get(0)).get("message"));
        String content = String.valueOf(message.get("content"));
        return JSON.readValue(content, Map.class);
    }

    // =====================================================================
    //  프롬프트
    // =====================================================================
    private String systemPrompt() {
        return """
               너는 '냉장고 파머'의 요리 추천 도우미다. 사용자가 보유한 냉장고 재료를 최대한 활용해
               집에서 쉽게 만들 수 있는 한국식 가정 요리 1개를 추천한다.
               반드시 아래 JSON 형식 '하나의 객체'로만 응답한다(설명/마크다운 금지):
               {
                 "title": "요리 이름(한국어)",
                 "summary": "한두 문장 소개",
                 "ingredients": [ { "name": "재료명", "qty": 2, "unit": "개" } ],
                 "steps": [ { "stepNumber": 1, "description": "조리 설명" } ],
                 "meta": { "cookTime": 20, "difficulty": "EASY", "servings": 2 }
               }
               difficulty는 EASY|MEDIUM|HARD 중 하나. qty는 숫자, unit은 문자열.
               """;
    }

    private String userPrompt(List<FridgeItem> fridge, String allergies, AiRecommendRequest req,
                              List<RecipeMatchCandidate> references) {
        StringBuilder sb = new StringBuilder();
        sb.append("냉장고 재료 목록(이름 / 수량 / 보관 / D-day):\n");
        for (FridgeItem f : fridge) {
            sb.append("- ").append(f.name())
              .append(" / ").append(f.qty()).append(f.unit() == null ? "" : f.unit())
              .append(" / ").append(f.storageType())
              .append(" / D").append(f.dDay() >= 0 ? "+" + f.dDay() : String.valueOf(f.dDay()))
              .append("\n");
        }
        if (req.prioritizeExpiryOrDefault()) {
            sb.append("\n유통기한이 임박한(D-").append(NEAR_EXPIRY_DAYS)
              .append(" 이내) 재료를 우선적으로 소비하는 레시피를 우선한다.\n");
        }
        if (!req.useAllFridgeOrDefault()) {
            sb.append("모든 재료를 다 쓸 필요는 없고, 잘 어울리는 일부만 골라 사용해도 된다.\n");
        }
        if (allergies != null && !allergies.isBlank()) {
            sb.append("\n다음 알레르기 재료는 절대 사용하지 말 것: ").append(allergies).append("\n");
        }
        sb.append("\n부족한 핵심 재료가 있으면 일반적으로 쉽게 구할 수 있는 것만 추가한다.");

        // 참고: 보유 재료와 일부 겹치는 기존(DB) 레시피 제목 — 영감용(그대로 베끼지 말 것)
        if (references != null && !references.isEmpty()) {
            String titles = references.stream()
                    .limit(5)
                    .map(RecipeMatchCandidate::title)
                    .collect(Collectors.joining(", "));
            sb.append("\n\n참고로, 냉장고 재료와 일부 겹치는 기존 레시피들: ").append(titles)
              .append("\n위는 참고만 하고, 보유 재료에 더 잘 맞게 자유롭게 제안해도 된다.");
        }

        // 다양성: 매 호출 무작위 스타일을 힌트로 주어 같은 재료로도 다른 요리를 제안하게 한다.
        String style = STYLE_HINTS[ThreadLocalRandom.current().nextInt(STYLE_HINTS.length)];
        sb.append("\n\n이번에는 '").append(style)
          .append("' 계열로 색다르게 제안해줘. 가장 뻔한 한 가지 요리(예: 김치볶음밥)는 가급적 피하고, ")
          .append("재료에 어울리는 창의적인 메뉴를 골라줘.");
        return sb.toString();
    }

    // =====================================================================
    //  SSE / 캐스팅 유틸
    // =====================================================================
    private void send(SseEmitter emitter, String type, Object value) throws Exception {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", type);
        if (value != null) {
            event.put("value", value);
        }
        emitter.send(SseEmitter.event().data(event, MediaType.APPLICATION_JSON));
    }

    private void sendErrorQuietly(SseEmitter emitter, String message) {
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("type", "error");
            event.put("value", message == null ? "AI 추천 중 오류가 발생했습니다" : message);
            event.put("retryable", true);
            emitter.send(SseEmitter.event().data(event, MediaType.APPLICATION_JSON));
        } catch (Exception ignore) {
            // 이미 끊긴 연결 등 — 무시
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object> asList(Object o) {
        return (o instanceof List<?>) ? (List<Object>) o : new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object o) {
        return (o instanceof Map<?, ?>) ? (Map<String, Object>) o : new LinkedHashMap<>();
    }
}
