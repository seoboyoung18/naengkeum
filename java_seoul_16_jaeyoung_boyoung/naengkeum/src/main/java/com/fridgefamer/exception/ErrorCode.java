package com.fridgefamer.exception;

import org.springframework.http.HttpStatus;

/**
 * 9종 공통 에러 코드 (WBS-⑤ API 명세서 결정사항 반영).
 *
 * <p>구조: HTTP 상태 + 클라이언트용 code 문자열 + 기본 메시지.
 * GlobalExceptionHandler에서 ApiException을 잡으면 이 정보로 응답을 만든다.</p>
 *
 * <p>API 공통 응답 포맷:
 * <pre>
 * {
 *   "code": "TOKEN_EXPIRED",
 *   "message": "세션이 만료되었습니다"
 * }
 * </pre></p>
 */
public enum ErrorCode {

    // ===== 400 =====
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "입력값이 유효하지 않습니다"),
    BAD_REQUEST     (HttpStatus.BAD_REQUEST, "BAD_REQUEST",      "잘못된 요청입니다"),

    // ===== 401 =====
    UNAUTHORIZED    (HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",    "인증 정보가 필요합니다"),
    TOKEN_EXPIRED   (HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED",   "세션이 만료되었습니다"),
    INVALID_TOKEN   (HttpStatus.UNAUTHORIZED, "INVALID_TOKEN",   "유효하지 않은 토큰입니다"),

    // ===== 403 =====
    FORBIDDEN       (HttpStatus.FORBIDDEN,    "FORBIDDEN",       "권한이 없습니다"),

    // ===== 404 =====
    NOT_FOUND       (HttpStatus.NOT_FOUND,    "NOT_FOUND",       "리소스를 찾을 수 없습니다"),

    // ===== 409 =====
    CONFLICT        (HttpStatus.CONFLICT,     "CONFLICT",        "이미 존재하는 데이터입니다"),

    // ===== 500/503 =====
    INTERNAL_ERROR  (HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",   "서버 내부 오류가 발생했습니다"),
    AI_SERVICE_ERROR(HttpStatus.SERVICE_UNAVAILABLE,   "AI_SERVICE_ERROR", "AI 서비스 호출에 실패했습니다");

    private final HttpStatus status;
    private final String code;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String code, String defaultMessage) {
        this.status = status;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus()         { return status; }
    public String     getCode()           { return code; }
    public String     getDefaultMessage() { return defaultMessage; }
}
