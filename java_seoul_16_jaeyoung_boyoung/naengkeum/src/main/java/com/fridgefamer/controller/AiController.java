package com.fridgefamer.controller;

import com.fridgefamer.dto.request.ai.AiCoachingRequest;
import com.fridgefamer.dto.request.ai.AiRecommendRequest;
import com.fridgefamer.service.AiCoachingService;
import com.fridgefamer.service.AiRecommendService;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI(AI 추천/코칭) API — API 명세 §9.
 *
 * <ul>
 *   <li>POST /api/ai/recommend — F19 냉장고 기반 레시피 추천 (SSE 스트리밍)</li>
 *   <li>POST /api/ai/coaching  — F20 식재료 보관/활용 코칭 (SSE 스트리밍)</li>
 * </ul>
 *
 * <p>인증 필요(SecurityConfig anyRequest().authenticated()). 추천은 빈 냉장고 400,
 * 코칭은 재료명 누락 400, 공통으로 AI 키 미설정 503은 연결 전 동기 차단되고,
 * 스트리밍 도중 오류는 error 이벤트로 전달된다.</p>
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiRecommendService aiRecommendService;
    private final AiCoachingService aiCoachingService;

    public AiController(AiRecommendService aiRecommendService,
                        AiCoachingService aiCoachingService) {
        this.aiRecommendService = aiRecommendService;
        this.aiCoachingService = aiCoachingService;
    }

    @PostMapping(value = "/recommend", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter recommend(@RequestBody(required = false) AiRecommendRequest req) {
        AiRecommendRequest safe = (req != null)
                ? req
                : new AiRecommendRequest(null, null, null);
        return aiRecommendService.recommend(currentMemberId(), safe);
    }

    @PostMapping(value = "/coaching", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter coaching(@RequestBody(required = false) AiCoachingRequest req) {
        return aiCoachingService.coaching(currentMemberId(), req);
    }

    /** 인증 필수. */
    private Long currentMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}