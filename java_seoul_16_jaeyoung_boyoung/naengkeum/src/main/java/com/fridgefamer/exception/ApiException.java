package com.fridgefamer.exception;

/**
 * 비즈니스 로직에서 사용하는 커스텀 예외.
 *
 * <p>Service 계층에서 ErrorCode와 함께 throw하면
 * GlobalExceptionHandler가 자동으로 통일된 JSON 응답으로 변환한다.</p>
 *
 * <p>사용 예시:
 * <pre>
 * if (memberMapper.findByEmail(email) != null) {
 *     throw new ApiException(ErrorCode.CONFLICT, "이미 사용 중인 이메일입니다");
 * }
 *
 * Member m = memberMapper.findById(id);
 * if (m == null) {
 *     throw new ApiException(ErrorCode.NOT_FOUND);  // 기본 메시지 사용
 * }
 * </pre></p>
 *
 * <p>RuntimeException 상속이라 try-catch 강제하지 않음 → 컨트롤러에서 자유롭게 던질 수 있음.</p>
 */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    /** 기본 메시지 사용 */
    public ApiException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    /** 커스텀 메시지 사용 (DB의 어떤 컬럼이 중복인지 등 상세 정보) */
    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /** 원인 예외와 함께 (디버깅 로그에 stacktrace 유지) */
    public ApiException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
