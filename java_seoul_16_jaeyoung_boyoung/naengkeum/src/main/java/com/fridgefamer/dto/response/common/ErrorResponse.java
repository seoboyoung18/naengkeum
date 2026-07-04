package com.fridgefamer.dto.response.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fridgefamer.exception.ErrorCode;

import java.util.Map;

/**
 * 공통 에러 응답 DTO.
 *
 * <p>9종 ErrorCode + 메시지 + (선택) 필드별 검증 에러를 담는다.
 * GlobalExceptionHandler에서 모든 예외를 이 형태로 변환하여 응답.</p>
 *
 * <p>응답 예시:
 * <pre>
 * // 일반 에러
 * { "code": "UNAUTHORIZED", "message": "인증 정보가 필요합니다" }
 *
 * // 필드 검증 에러
 * {
 *   "code": "VALIDATION_ERROR",
 *   "message": "입력값 검증에 실패했습니다",
 *   "fieldErrors": {
 *     "email": "올바른 이메일 형식이 아닙니다",
 *     "password": "비밀번호는 8자 이상이어야 합니다"
 *   }
 * }
 * </pre></p>
 *
 * <p>{@code @JsonInclude(NON_NULL)}: fieldErrors가 null일 때 JSON에서 생략 →
 * 일반 에러 응답이 깔끔해진다.</p>
 *
 * <p>record 사용 이유: 불변 DTO + 보일러플레이트 제거 (Java 16+).</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        Map<String, String> fieldErrors
) {

    /** 일반 에러용 — fieldErrors 없이 */
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getDefaultMessage(), null);
    }

    /** 커스텀 메시지가 필요한 경우 */
    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.getCode(), message, null);
    }

    /** Validation 에러용 — fieldErrors 포함 */
    public static ErrorResponse of(ErrorCode errorCode, String message,
                                   Map<String, String> fieldErrors) {
        return new ErrorResponse(errorCode.getCode(), message, fieldErrors);
    }
}