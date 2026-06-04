package com.fridgefamer.service;

import com.fridgefamer.dto.request.ai.AiRecommendRequest;
import com.fridgefamer.dto.response.fridge.FridgeItem;
import com.fridgefamer.exception.ApiException;
import com.fridgefamer.exception.ErrorCode;
import com.fridgefamer.mapper.ai.AiMapper;
import com.fridgefamer.mapper.fridge.FridgeMapper;
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

    private final FridgeMapper fridgeMapper;
    private final AiMapper aiMapper;
    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public AiRecommendService(FridgeMapper fridgeMapper,
                              AiMapper aiMapper,
                              @Value("${openai.base-url}") String baseUrl,
                              @Value("${openai.api-key}") String apiKey,
                              @Value("${openai.model}") String model) {
        this.fridgeMapper = fridgeMapper;
        this.aiMapper = aiMapper;
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

            Map<String, Object> recipe = callOpenAi(fridge, allergies, req);

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
        } catch (Exception e) {
            log.warn("AI recommend stream failed: {}", e.getMessage());
            sendErrorQuietly(emitter, e.getMessage());
            emitter.complete();
        }
    }

    // =====================================================================
    //  OpenAI Chat Completions (JSON 모드) 호출 → 파싱된 레시피 Map
    // =====================================================================
    @SuppressWarnings("unchecked")
    private Map<String, Object> callOpenAi(List<FridgeItem> fridge, String allergies, AiRecommendRequest req) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", 0.7);
        body.put("response_format", Map.of("type", "json_object"));
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt()),
                Map.of("role", "user", "content", userPrompt(fridge, allergies, req))));

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

    private String userPrompt(List<FridgeItem> fridge, String allergies, AiRecommendRequest req) {
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
