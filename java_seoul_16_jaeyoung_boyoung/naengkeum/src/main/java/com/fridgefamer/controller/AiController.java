package com.fridgefamer.controller;

import com.fridgefamer.dto.request.ai.CoachingRequest;
import com.fridgefamer.dto.request.ai.RecommendRequest;
import com.fridgefamer.service.AiCoachingService;
import com.fridgefamer.service.AiRecommendService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI API — API 명세 §9. 전부 인증 필요, SSE(text/event-stream) 응답.
 *
 * <ul>
 *   <li>POST /api/ai/recommend — F19 냉장고 재료 기반 레시피 추천 (SSE)</li>
 *   <li>POST /api/ai/coaching  — F20 식재료 보관/활용 코칭 (SSE)</li>
 * </ul>
 *
 * <p>produces=text/event-stream으로 SSE 스트림 반환. 실제 LLM 호출은 서비스의
 * 가상 스레드에서 비동기 처리되고, 이벤트가 준비되는 대로 클라이언트로 전송된다.</p>
 *
 * <p>SecurityConfig에 /api/ai/** 를 인증 필요로 두어야 한다(화이트리스트에 넣지 말 것).</p>
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiRecommendService recommendService;
    private final AiCoachingService coachingService;

    public AiController(AiRecommendService recommendService,
                        AiCoachingService coachingService) {
        this.recommendService = recommendService;
        this.coachingService = coachingService;
    }

    @PostMapping(value = "/recommend", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter recommend(@RequestBody RecommendRequest req) {
        return recommendService.recommend(currentMemberId(), req);
    }

    @PostMapping(value = "/coaching", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter coaching(@Valid @RequestBody CoachingRequest req) {
        return coachingService.coaching(currentMemberId(), req);
    }

    /** 인증 필수 — principal은 Long memberId. */
    private Long currentMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}