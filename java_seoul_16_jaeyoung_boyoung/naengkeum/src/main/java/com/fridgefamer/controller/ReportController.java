package com.fridgefamer.controller;

import com.fridgefamer.dto.request.report.CreateReportRequest;
import com.fridgefamer.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 신고(Report) API.
 *
 * <ul>
 *   <li>POST /api/report — 레시피/리뷰 신고(인증 필요). 중복 신고 409.</li>
 * </ul>
 *
 * <p>SecurityConfig의 anyRequest().authenticated()로 보호된다(별도 permitAll 없음).</p>
 */
@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> create(@Valid @RequestBody CreateReportRequest req) {
        reportService.create(currentMemberId(), req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "신고가 접수되었습니다"));
    }

    /** 인증 필수. SecurityConfig에서도 막지만 이중 방어. */
    private Long currentMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
