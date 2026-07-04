/**
 * Business Logic Layer — 트랜잭션 경계.
 * <p>도메인별 Service는 Mapper를 호출하여 데이터 가공·검증을 수행한다.
 * 예외 상황은 ApiException(ErrorCode.XXX)로 던져 GlobalExceptionHandler가 처리.</p>
 * @see com.fridgefamer.mapper
 * @see com.fridgefamer.exception.ApiException
 */
package com.fridgefamer.service;
