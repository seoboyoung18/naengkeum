package com.fridgefamer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fridgefamer.dto.request.ai.RecommendRequest;
import com.fridgefamer.dto.response.fridge.FridgeItem;
import com.fridgefamer.exception.ApiException;
import com.fridgefamer.exception.ErrorCode;
import com.fridgefamer.llm.LlmClient;
import com.fridgefamer.llm.LlmException;
import com.fridgefamer.mapper.fridge.FridgeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 레시피 추천 서비스 — API 명세 §9 F19 (SSE).
 *
 * <p>흐름: 냉장고 재료 조회 → 프롬프트 구성 → LLM 호출(논스트리밍) →
 * 받은 레시피 JSON을 SSE 이벤트로 쪼개 전송(title→summary→ingredient*→step*→meta→done).</p>
 *
 * <p>LLM은 LlmClient 추상화에 의존(GMS 또는 Mock). 실제 토큰 스트리밍 대신
 * "완성 받아 SSE로 연출"하는 PoC 방식이라, 프론트는 점진적 렌더링 경험을 얻는다.</p>
 */
@Service
public class AiRecommendService {

    private static final Logger log = LoggerFactory.getLogger(AiRecommendService.class);

    private final FridgeMapper fridgeMapper;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public AiRecommendService(FridgeMapper fridgeMapper,
                              LlmClient llmClient,
                              ObjectMapper objectMapper) {
        this.fridgeMapper = fridgeMapper;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 추천을 SSE로 스트리밍. 별도 스레드에서 LLM 호출 후 이벤트 전송.
     * @return 즉시 반환되는 SseEmitter (실제 전송은 비동기)
     */
    public SseEmitter recommend(Long memberId, RecommendRequest req) {
        // 냉장고 재료 조회 (전체, sort=null → 기본 정렬 expiry_date ASC = 유통기한 임박순)
        List<FridgeItem> fridge = fridgeMapper.selectByMember(memberId, null, null);
        if (fridge.isEmpty()) {
            // 연결 전 단계 오류는 SSE가 아니라 일반 예외(400)로
            throw new ApiException(ErrorCode.BAD_REQUEST, "냉장고에 재료가 없습니다. 재료를 먼저 추가해주세요.");
        }

        // 타임아웃 0 = 무제한(개발 편의). 운영은 적절히 설정.
        SseEmitter emitter = new SseEmitter(60_000L);

        // 별도 스레드에서 LLM 호출 + 스트리밍 (요청 스레드 블로킹 방지)
        // Java 17 호환을 위해 일반 스레드 사용 (가상 스레드는 21+).
        Thread worker = new Thread(() -> streamRecommendation(emitter, fridge, req));
        worker.setDaemon(true);
        worker.start();

        return emitter;
    }

    private void streamRecommendation(SseEmitter emitter, List<FridgeItem> fridge, RecommendRequest req) {
        try {
            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(fridge, req);

            String resultJson = llmClient.complete(systemPrompt, userPrompt);
            JsonNode recipe = parseRecipe(resultJson);

            // title
            sendEvent(emitter, "title", recipe.path("title").asText(""));
            // summary
            sendEvent(emitter, "summary", recipe.path("summary").asText(""));
            // ingredients (배열을 한 줄씩)
            for (JsonNode ing : recipe.path("ingredients")) {
                sendEventRaw(emitter, "ingredient", ing);
            }
            // steps (배열을 한 줄씩)
            for (JsonNode step : recipe.path("steps")) {
                sendEventRaw(emitter, "step", step);
            }
            // meta (cookTime/difficulty/servings)
            if (recipe.has("meta")) {
                sendEventRaw(emitter, "meta", recipe.path("meta"));
            }
            // done
            sendEvent(emitter, "done", "");
            emitter.complete();

        } catch (LlmException e) {
            // 스트리밍 도중 LLM 오류 → SSE error 이벤트로 전달
            log.warn("추천 스트리밍 중 LLM 오류: {}", e.getMessage());
            sendError(emitter, e.getMessage(), e.isRetryable());
            emitter.complete();
        } catch (Exception e) {
            log.error("추천 스트리밍 실패", e);
            sendError(emitter, "추천 생성 중 오류가 발생했습니다", false);
            emitter.complete();
        }
    }

    // =====================================================================
    //  프롬프트 구성
    // =====================================================================

    private String buildSystemPrompt() {
        return """
            너는 냉장고 재료로 만들 수 있는 레시피를 추천하는 요리 전문가야.
            반드시 아래 JSON 형식으로만 응답해. 다른 설명은 절대 붙이지 마.
            {
              "title": "레시피명",
              "summary": "한 줄 소개",
              "ingredients": [{"name":"재료명","qty":숫자,"unit":"단위","owned":true/false}],
              "steps": [{"stepNumber":1,"description":"조리 설명"}],
              "meta": {"cookTime":분, "difficulty":"EASY|MEDIUM|HARD", "servings":인분},
              "nutrition": {"calories":수,"carbs":수,"protein":수,"fat":수,"sodium":수}
            }
            owned는 사용자가 보유한 재료면 true, 추가로 사야 하면 false로 표시해.
            """;
    }

    private String buildUserPrompt(List<FridgeItem> fridge, RecommendRequest req) {
        String ingredients = fridge.stream()
                .map(f -> f.name() + "(" + f.qty() + f.unit()
                        + ", D" + (f.dDay() >= 0 ? "-" + f.dDay() : "+" + (-f.dDay())) + ")")
                .collect(Collectors.joining(", "));

        StringBuilder sb = new StringBuilder();
        sb.append("내 냉장고 재료: ").append(ingredients).append("\n");
        if (Boolean.TRUE.equals(req.prioritizeExpiry())) {
            sb.append("유통기한이 임박한 재료(D-day가 작은 것)를 우선 사용해줘.\n");
        }
        if (Boolean.TRUE.equals(req.useAllFridge())) {
            sb.append("가능하면 냉장고 재료를 최대한 많이 활용해줘.\n");
        }
        sb.append("이 재료들로 만들 수 있는 레시피 1개를 추천해줘.");
        return sb.toString();
    }

    // =====================================================================
    //  SSE 전송 헬퍼
    // =====================================================================

    /** {"type":..., "value":문자열} 형태 이벤트 전송. */
    private void sendEvent(SseEmitter emitter, String type, String value) throws IOException {
        emitter.send(SseEmitter.event().data(
                objectMapper.writeValueAsString(java.util.Map.of("type", type, "value", value))));
    }

    /** {"type":..., "value":JSON객체} 형태 이벤트 전송. */
    private void sendEventRaw(SseEmitter emitter, String type, JsonNode value) throws IOException {
        var node = objectMapper.createObjectNode();
        node.put("type", type);
        node.set("value", value);
        emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(node)));
    }

    /** error 이벤트(retryable 포함). 전송 자체 실패는 무시(이미 끊긴 연결). */
    private void sendError(SseEmitter emitter, String message, boolean retryable) {
        try {
            emitter.send(SseEmitter.event().data(
                    objectMapper.writeValueAsString(java.util.Map.of(
                            "type", "error", "value", message, "retryable", retryable))));
        } catch (IOException ignored) {
            // 연결이 이미 끊긴 경우 — 무시
        }
    }

    private JsonNode parseRecipe(String json) {
        try {
            // LLM이 ```json ... ``` 코드펜스를 붙이는 경우 제거
            String cleaned = json.trim()
                    .replaceAll("^```json\\s*", "")
                    .replaceAll("^```\\s*", "")
                    .replaceAll("\\s*```$", "");
            return objectMapper.readTree(cleaned);
        } catch (Exception e) {
            throw new LlmException("레시피 JSON 파싱 실패", false, e);
        }
    }
}