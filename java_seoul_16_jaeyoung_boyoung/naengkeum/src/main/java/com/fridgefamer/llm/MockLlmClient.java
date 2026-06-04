package com.fridgefamer.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Mock LLM 클라이언트 — GMS 키가 없을 때 폴백.
 *
 * <p>GmsLlmClient(@ConditionalOnProperty "gms.api-key")가 등록되지 않으면
 * (@ConditionalOnMissingBean) 이 빈이 대신 주입된다. 키 없이도 SSE 스트리밍 흐름을
 * 검증할 수 있도록 그럴듯한 레시피/코칭 JSON을 반환한다.</p>
 *
 * <p>userPrompt에 "코칭"이 포함되면 코칭용 JSON, 아니면 레시피용 JSON을 돌려준다.
 * 실제 LLM처럼 입력 재료를 반영하진 않지만, 형식은 동일해 프론트 연동 테스트에 충분.</p>
 */
@Component
@ConditionalOnMissingBean(GmsLlmClient.class)
public class MockLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(MockLlmClient.class);

    public MockLlmClient() {
        log.info("MockLlmClient 활성화 — GMS 키가 없어 Mock 응답을 사용합니다");
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        boolean isCoaching = userPrompt != null && userPrompt.contains("코칭");
        return isCoaching ? mockCoachingJson() : mockRecipeJson();
    }

    private String mockRecipeJson() {
        return """
            {
              "title": "두부 계란 볶음",
              "summary": "냉장고 속 두부와 계란으로 만드는 간단하고 든든한 한 끼입니다.",
              "ingredients": [
                {"name": "두부", "qty": 1, "unit": "모", "owned": true},
                {"name": "계란", "qty": 2, "unit": "개", "owned": true},
                {"name": "대파", "qty": 1, "unit": "대", "owned": true},
                {"name": "간장", "qty": 1, "unit": "큰술", "owned": false}
              ],
              "steps": [
                {"stepNumber": 1, "description": "두부는 깍둑썰기하고 키친타월로 물기를 제거합니다."},
                {"stepNumber": 2, "description": "달군 팬에 기름을 두르고 두부를 노릇하게 굽습니다."},
                {"stepNumber": 3, "description": "계란을 풀어 넣고 대파와 간장을 더해 볶아 완성합니다."}
              ],
              "meta": {"cookTime": 15, "difficulty": "EASY", "servings": 2},
              "nutrition": {"calories": 320, "carbs": 12, "protein": 24, "fat": 18, "sodium": 580}
            }
            """;
    }

    private String mockCoachingJson() {
        return """
            {
              "storage": "계란은 뾰족한 쪽이 아래로 가도록 냉장 보관하면 신선도가 오래 유지됩니다. 씻지 말고 보관하세요.",
              "combo": "계란은 두부, 대파와 잘 어울립니다. 간단한 계란말이나 두부 계란 볶음으로 활용해 보세요."
            }
            """;
    }

    @Override
    public boolean isLive() {
        return false;
    }
}