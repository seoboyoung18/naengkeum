package com.fridgefamer.exception;

import com.fridgefamer.dto.response.common.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 전역 예외 처리기 — 모든 Controller의 throw된 예외를 통일된 JSON 응답으로 변환.
 *
 * <p>처리 순서 (구체적 → 일반적):
 * <ol>
 *   <li>ApiException                       — 비즈니스 명시적 throw (가장 흔함)</li>
 *   <li>MethodArgumentNotValidException    — @Valid Body 검증 실패</li>
 *   <li>ConstraintViolationException       — @PathVariable / @RequestParam 검증</li>
 *   <li>HttpMessageNotReadableException    — JSON 파싱 실패</li>
 *   <li>MethodArgumentTypeMismatchException — URL 변수 타입 변환 실패</li>
 *   <li>DuplicateKeyException              — MySQL 1062 (UNIQUE 위반)</li>
 *   <li>DataIntegrityViolationException    — MySQL CHECK/FK 등 무결성 위반</li>
 *   <li>NoResourceFoundException           — 매핑 안 된 URL (404)</li>
 *   <li>HttpRequestMethodNotSupportedException — 잘못된 HTTP 메서드</li>
 *   <li>Exception                          — Fallback (예상 못한 모든 에러)</li>
 * </ol></p>
 *
 * <p>모든 핸들러는 ErrorResponse DTO를 반환하며, Stacktrace는 서버 로그에만 기록한다.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // =================================================================
    // 1. 비즈니스 예외 — 가장 빈번하고 가장 중요한 케이스
    // =================================================================
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException e) {
        ErrorCode code = e.getErrorCode();
        log.warn("ApiException: code={}, message={}", code.getCode(), e.getMessage());

        return ResponseEntity
                .status(code.getStatus())
                .body(ErrorResponse.of(code, e.getMessage()));
    }

    // =================================================================
    // 2. @Valid Body 검증 실패 — 가장 자주 만나는 400 케이스
    // =================================================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBeanValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            // 같은 필드에 여러 검증 실패 시 첫 번째 메시지만 사용
            fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        log.warn("Validation failed: {}", fieldErrors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        ErrorCode.VALIDATION_ERROR,
                        "입력값 검증에 실패했습니다",
                        fieldErrors
                ));
    }

    // =================================================================
    // 3. @PathVariable / @RequestParam 검증 실패
    // =================================================================
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        e.getConstraintViolations().forEach(cv -> {
            // path 예: "getMember.userId" → "userId"만 추출
            String path = cv.getPropertyPath().toString();
            String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            fieldErrors.put(field, cv.getMessage());
        });
        log.warn("Constraint violation: {}", fieldErrors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        ErrorCode.VALIDATION_ERROR,
                        "요청 파라미터가 유효하지 않습니다",
                        fieldErrors
                ));
    }

    // =================================================================
    // 4. JSON 파싱 실패 — Body가 잘못된 형식이거나 비어있을 때
    // =================================================================
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonParse(HttpMessageNotReadableException e) {
        log.warn("JSON parse error: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        ErrorCode.BAD_REQUEST,
                        "요청 본문 형식이 올바르지 않습니다"
                ));
    }

    // =================================================================
    // 5. URL 변수 타입 변환 실패 — "/api/member/abc" 같은 케이스 (Long 자리에 String)
    // =================================================================
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = String.format("'%s' 값이 올바른 타입이 아닙니다", e.getName());
        log.warn("Type mismatch: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.BAD_REQUEST, message));
    }

    // =================================================================
    // 6. MySQL UNIQUE 제약 위반 (Error 1062) → 409 자동 변환
    //    예: 이메일/닉네임/리뷰 중복
    // =================================================================
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("Duplicate key: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(ErrorCode.CONFLICT));
    }

    // =================================================================
    // 7. MySQL CHECK 제약 / FK 위반 → 400
    //    예: rating=6, qty=-1, recipe_id+ai_recipe_id 모두 NULL 등
    // =================================================================
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("Data integrity violation: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        ErrorCode.VALIDATION_ERROR,
                        "데이터 무결성 제약 조건에 위반됩니다"
                ));
    }

    // =================================================================
    // 8. 매핑 안 된 URL → 404
    //    Spring Boot 4 기본 동작은 빈 응답인데, 의도적으로 JSON으로 변환
    // =================================================================
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        log.warn("No resource found: {}", e.getResourcePath());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(
                        ErrorCode.NOT_FOUND,
                        "요청하신 경로를 찾을 수 없습니다: /" + e.getResourcePath()
                ));
    }

    // =================================================================
    // 9. 잘못된 HTTP 메서드 → 405
    //    예: GET 엔드포인트에 POST 요청
    // =================================================================
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("Method not supported: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponse.of(
                        ErrorCode.BAD_REQUEST,
                        String.format("'%s' 메서드는 지원되지 않습니다", e.getMethod())
                ));
    }

    // =================================================================
    // 10. Fallback — 위의 모든 케이스에 안 잡힌 예외
    //     서버 내부 정보 노출 방지를 위해 사용자에게는 안전한 메시지만 반환
    // =================================================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        // 운영 환경에서 stacktrace는 로그에만 남기고 응답에는 미포함
        log.error("Unexpected error", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
    }
}