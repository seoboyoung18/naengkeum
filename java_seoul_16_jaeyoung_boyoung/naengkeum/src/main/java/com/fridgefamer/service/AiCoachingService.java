package com.fridgefamer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fridgefamer.dto.request.ai.CoachingRequest;
import com.fridgefamer.llm.LlmClient;
import com.fridgefamer.llm.LlmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

/**
 * AI 식재료 코칭 서비스 — API 명세 §9 F20 (SSE).
 *
 * <p>특정 재료의 보관법(storage) + 활용 조합(combo)을 LLM에게 받아 SSE로 전송.
 * 추천과 동일하게 논스트리밍 LLM 호출 후 이벤트로 쪼갠다.</p>
 */
@Service
public class AiCoachingService {

    private static final Logger log = LoggerFactory.getLogger(AiCoachingService.class);

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public AiCoachingService(LlmClient llmClient, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    public SseEmitter coaching(Long memberId, CoachingRequest req) {
        SseEmitter emitter = new SseEmitter(60_000L);
        // Java 17 호환을 위해 일반 스레드 사용 (가상 스레드는 21+).
        Thread worker = new Thread(() -> streamCoaching(emitter, req));
        worker.setDaemon(true);
        worker.start();
        return emitter;
    }

    private void streamCoaching(SseEmitter emitter, CoachingRequest req) {
        try {
            String systemPrompt = """
                너는 식재료 보관과 활용을 알려주는 전문가야.
                반드시 아래 JSON 형식으로만 응답해. 다른 설명은 붙이지 마.
                {
                  "storage": "보관법 설명 (2~3문장)",
                  "combo": "이 재료와 어울리는 음식/조합 추천 (2~3문장)"
                }
                요청에 '코칭'이라는 단어가 포함될 수 있어.
                """;
            String userPrompt = "재료 '" + req.ingredientName() + "'에 대한 보관법과 활용 조합을 코칭해줘.";

            String resultJson = llmClient.complete(systemPrompt, userPrompt);
            JsonNode coaching = parse(resultJson);

            sendEvent(emitter, "storage", coaching.path("storage").asText(""));
            sendEvent(emitter, "combo", coaching.path("combo").asText(""));
            sendEvent(emitter, "done", "");
            emitter.complete();

        } catch (LlmException e) {
            log.warn("코칭 스트리밍 중 LLM 오류: {}", e.getMessage());
            sendError(emitter, e.getMessage(), e.isRetryable());
            emitter.complete();
        } catch (Exception e) {
            log.error("코칭 스트리밍 실패", e);
            sendError(emitter, "코칭 생성 중 오류가 발생했습니다", false);
            emitter.complete();
        }
    }

    // ---- SSE 전송 헬퍼 (AiRecommendService와 동일 패턴) ----

    private void sendEvent(SseEmitter emitter, String type, String value) throws IOException {
        emitter.send(SseEmitter.event().data(
                objectMapper.writeValueAsString(Map.of("type", type, "value", value))));
    }

    private void sendError(SseEmitter emitter, String message, boolean retryable) {
        try {
            emitter.send(SseEmitter.event().data(
                    objectMapper.writeValueAsString(Map.of(
                            "type", "error", "value", message, "retryable", retryable))));
        } catch (IOException ignored) {
        }
    }

    private JsonNode parse(String json) {
        try {
            String cleaned = json.trim()
                    .replaceAll("^```json\\s*", "")
                    .replaceAll("^```\\s*", "")
                    .replaceAll("\\s*```$", "");
            return objectMapper.readTree(cleaned);
        } catch (Exception e) {
            throw new LlmException("코칭 JSON 파싱 실패", false, e);
        }
    }
}