package com.fridgefamer.llm;

/**
 * LLM 호출 추상화 — 제공사(GMS/OpenAI/Gemini/Mock)에 무관하게 동일 인터페이스.
 *
 * <p>Service는 이 인터페이스에만 의존한다. 실제 구현체(GmsLlmClient)를 교체하거나
 * 키가 없을 때 MockLlmClient로 폴백해도 Service 코드는 바뀌지 않는다.</p>
 *
 * <p>PoC 단순화: 스트리밍이 아니라 완성된 응답을 한 번에 받는다(논스트리밍).
 * 사용자에게 흘려보내는 SSE 연출은 Service 계층에서 응답을 쪼개 처리한다.</p>
 */
public interface LlmClient {

    /**
     * 프롬프트를 보내고 완성된 텍스트 응답을 받는다.
     *
     * @param systemPrompt 시스템 역할 지시(레시피 형식 등). null 가능.
     * @param userPrompt   사용자 입력(냉장고 재료/요청).
     * @return LLM이 생성한 텍스트(보통 JSON 문자열).
     * @throws LlmException 호출 실패(타임아웃/인증/5xx 등).
     */
    String complete(String systemPrompt, String userPrompt);

    /** 이 클라이언트가 실제 LLM에 연결되는지(true) Mock인지(false). 로깅/디버깅용. */
    boolean isLive();
}