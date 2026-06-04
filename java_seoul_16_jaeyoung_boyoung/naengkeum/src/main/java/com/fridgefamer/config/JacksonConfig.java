package com.fridgefamer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson ObjectMapper 빈 등록.
 *
 * <p>이 프로젝트는 spring-boot-starter-webmvc만 사용해 Jackson 자동 설정이
 * ObjectMapper 빈을 등록하지 않는 구성이다(그래서 RecipeDataLoader 등은 직접 new로
 * 생성해 왔다). AI 도메인(GmsLlmClient/AiRecommendService/AiCoachingService)이
 * ObjectMapper를 주입받으므로, 전역 재사용 가능한 단일 빈으로 등록한다.</p>
 *
 * <p>ObjectMapper는 thread-safe하여 싱글턴 빈으로 안전하게 공유된다.</p>
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}