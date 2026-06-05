package com.fridgefamer.service;

import com.fridgefamer.dto.request.ai.AiCoachingRequest;
import com.fridgefamer.exception.ApiException;
import com.fridgefamer.exception.ErrorCode;
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
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tools.jackson.databind.json.JsonMapper;

/**
 * AI 식재료 코칭 서비스 — API 명세 §9 F20. POST /api/ai/coaching (SSE).
 *
 * <p>재료명으로 보관법(storage) + 활용 조합(combo)을 OpenAI Chat Completions(JSON 모드)에서
 * 받아 SSE 이벤트(storage→combo→done)로 흘려보낸다. 재료명 누락은 연결 전 400,
 * 키 미설정은 연결 전 503으로 차단하고, 호출 도중 실패는 error 이벤트로 알린다.</p>
 *
 * <p>AiRecommendService와 동일 방침: ObjectMapper 빈에 의존하지 않고 로컬 JsonMapper로
 * 파싱하며, openai.* 프로퍼티 + RestClient + ExecutorService 구조를 따른다.</p>
 */
@Service
public class AiCoachingService {

    private static final Logger log = LoggerFactory.getLogger(AiCoachingService.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final long SSE_TIMEOUT_MS = 120_000L;

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public AiCoachingService(@Value("${openai.base-url}") String baseUrl,
                             @Value("${openai.api-key}") String apiKey,
                             @Value("${openai.model}") String model) {
        this.apiKey = apiKey;
        this.model = model;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    // =====================================================================
    //  POST /api/ai/coaching — 동기 사전검증 후 SSE 비동기 스트리밍 시작
    // =====================================================================
    public SseEmitter coaching(Long memberId, AiCoachingRequest req) {
        // 1) 연결 전 차단 — 재료명 누락(400), 키 미설정(503)
        if (req == null || !req.hasIngredientName()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "코칭할 재료명이 필요합니다.");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiException(ErrorCode.AI_SERVICE_ERROR, "AI 서비스가 설정되지 않았습니다.");
        }

        String ingredientName = req.ingredientName().trim();

        // 2) SSE 시작 — 이후 실패는 error 이벤트로 전달
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        executor.submit(() -> stream(emitter, ingredientName));
        return emitter;
    }

    // =====================================================================
    //  비동기 스트리밍 본체
    // =====================================================================
    private void stream(SseEmitter emitter, String ingredientName) {
        try {
            Map<String, Object> coaching = callOpenAi(ingredientName);

            Object storage = coaching.get("storage");
            Object combo = coaching.get("combo");

            send(emitter, "storage", storage != null ? storage : "보관 정보를 찾지 못했습니다.");
            send(emitter, "combo", combo != null ? combo : "추천 조합을 찾지 못했습니다.");
            send(emitter, "done", null);
            emitter.complete();
        } catch (Exception e) {
            log.warn("AI coaching stream failed: {}", e.getMessage());
            sendErrorQuietly(emitter, e.getMessage());
            emitter.complete();
        }
    }

    // =====================================================================
    //  OpenAI Chat Completions (JSON 모드) 호출 → 파싱된 코칭 Map
    // =====================================================================
    @SuppressWarnings("unchecked")
    private Map<String, Object> callOpenAi(String ingredientName) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", 0.7);
        body.put("response_format", Map.of("type", "json_object"));
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt()),
                Map.of("role", "user", "content", userPrompt(ingredientName))));

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
            너는 1인 가구를 위한 식재료 보관·활용 코치다.
            사용자가 알려준 재료에 대해 반드시 아래 JSON 형식으로만 응답한다. 다른 텍스트는 절대 붙이지 않는다.
            {
              "storage": "보관법 설명 (보관 위치/온도/기한/팁을 2~3문장, 한국어)",
              "combo": "이 재료와 어울리는 음식/조합 추천 (2~3문장, 한국어, 구체적인 메뉴 예시 포함)"
            }
            과장이나 의학적 단정은 피하고, 자취생이 실천하기 쉬운 현실적인 조언을 준다.
            """;
    }

    private String userPrompt(String ingredientName) {
        return "재료: '" + ingredientName + "'\n이 재료의 보관법(storage)과 활용 조합(combo)을 코칭해줘.";
    }

    // =====================================================================
    //  SSE / 캐스팅 유틸 (AiRecommendService와 동일 패턴)
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
            event.put("value", message == null ? "AI 코칭 중 오류가 발생했습니다" : message);
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