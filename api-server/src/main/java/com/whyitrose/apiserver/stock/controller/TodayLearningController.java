package com.whyitrose.apiserver.stock.controller;

import com.whyitrose.apiserver.stock.service.TodayLearningService;
import com.whyitrose.core.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stocks")
public class TodayLearningController {

    private final TodayLearningService todayLearningService;

    /**
     * 차트 핀 표시용 — 어제 digest 날짜 + 뉴스 건수 반환
     * 뉴스가 없으면 204 No Content (프론트에서 핀 미표시 처리)
     */
    @GetMapping("/{stockId}/learning-pin")
    public ResponseEntity<?> getLearningPin(@PathVariable Long stockId) {
        return todayLearningService.getLearningPin(stockId)
                .<ResponseEntity<?>>map(r -> ResponseEntity.ok(BaseResponse.success(r)))
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * 핀 클릭 시 사이드바용 — 어제 뉴스 + 가격 변화 + 예측 상태 반환
     * 뉴스가 없으면 204 No Content
     * 인증: 선택적 (비로그인도 조회 가능, 로그인+예측완료 시 prediction 포함)
     */
    @GetMapping("/{stockId}/today-learning")
    public ResponseEntity<?> getTodayLearning(@PathVariable Long stockId) {
        return todayLearningService.getTodayLearning(stockId)
                .<ResponseEntity<?>>map(r -> ResponseEntity.ok(BaseResponse.success(r)))
                .orElse(ResponseEntity.noContent().build());
    }
}
