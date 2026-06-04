package com.fridgefamer.llm;

/**
 * LLM 호출 실패 예외 — 타임아웃/인증/5xx 등.
 *
 * <p>Service에서 잡아 SSE error 이벤트로 변환하거나, 연결 전 단계면
 * ApiException(AI_SERVICE_ERROR)로 매핑한다. retryable로 재시도 가능 여부 표시.</p>
 */
public class LlmException extends RuntimeException {

    private final boolean retryable;

    public LlmException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public LlmException(String message, boolean retryable, Throwable cause) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}