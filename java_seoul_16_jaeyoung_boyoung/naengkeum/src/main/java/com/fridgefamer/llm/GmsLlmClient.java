package com.fridgefamer.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * GMS(SSAFY Gen AI Management System) 경유 LLM 호출 구현체.
 *
 * <p>GMS는 OpenAI/Gemini를 통합 프록시하는 SSAFY 플랫폼. base-url만 GMS로 바꾸면
 * OpenAI API와 동일한 형식으로 호출된다(가이드 기준).</p>
 *
 * <ul>
 *   <li>엔드포인트: https://gms.ssafy.io/gmsapi/api.openai.com/v1/chat/completions</li>
 *   <li>인증: Header "Authorization: Bearer {GMS_KEY}" (OpenAI 방식)</li>
 *   <li>모델: gpt-4.1-mini 등 (크레딧 절약). application.yml에서 주입.</li>
 * </ul>
 *
 * <p>gms.api-key가 설정되어 있을 때만 빈으로 등록된다(@ConditionalOnProperty).
 * 키가 없으면 MockLlmClient가 대신 주입된다.</p>
 */
@Component
@ConditionalOnProperty(name = "gms.api-key")
public class GmsLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(GmsLlmClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final String apiKey;

    public GmsLlmClient(
            @Value("${gms.base-url:https://gms.ssafy.io/gmsapi/api.openai.com/v1}") String baseUrl,
            @Value("${gms.api-key}") String apiKey,
            @Value("${gms.model:gpt-4.1-mini}") String model,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        log.info("GmsLlmClient 활성화 — model={}, baseUrl={}", model, baseUrl);
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        // OpenAI Chat Completions 형식 요청 본문
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt == null ? "" : systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.7
        );

        try {
            String raw = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return extractContent(raw);

        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            // 네트워크/타임아웃/5xx 등 — 재시도 가능으로 표시
            log.warn("GMS 호출 실패: {}", e.getMessage());
            throw new LlmException("GMS API 호출 실패: " + e.getMessage(), true, e);
        }
    }

    /** OpenAI 응답 JSON에서 choices[0].message.content 추출. */
    private String extractContent(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new LlmException("GMS 응답에 content가 없습니다", false);
            }
            return content.asText();
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("GMS 응답 파싱 실패: " + e.getMessage(), false, e);
        }
    }

    @Override
    public boolean isLive() {
        return true;
    }
}