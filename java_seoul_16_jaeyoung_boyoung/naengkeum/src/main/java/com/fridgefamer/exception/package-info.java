/**
 * Exception Handling — 9종 에러 코드 통일 응답.
 * <ul>
 *   <li>ErrorCode (enum) — 9종 에러 코드 정의</li>
 *   <li>ApiException — 비즈니스 로직에서 throw하는 RuntimeException</li>
 *   <li>GlobalExceptionHandler — @RestControllerAdvice로 모든 예외를 통일 응답으로 변환</li>
 * </ul>
 */
package com.fridgefamer.exception;
