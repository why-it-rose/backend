package com.whyitrose.apiserver.me.controller;

import com.whyitrose.apiserver.me.dto.MyPageStatsResponse;
import com.whyitrose.apiserver.me.dto.WeeklySummaryResponse;
import com.whyitrose.apiserver.prediction.service.PredictionService;
import com.whyitrose.core.exception.BaseException;
import com.whyitrose.core.response.BaseResponse;
import com.whyitrose.core.response.BaseResponseStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "me-controller", description = "마이페이지 API")
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final PredictionService predictionService;

    @Operation(summary = "마이페이지 통계 조회", description = "전체 예측 수, 정답률, 스크랩 수를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/stats")
    public ResponseEntity<BaseResponse<MyPageStatsResponse>> getMyStats(Authentication authentication) {
        Long userId = requireUserId(authentication);
        return ResponseEntity.ok(BaseResponse.success(predictionService.getMyStats(userId)));
    }

    @Operation(summary = "주간 예측 요약 조회", description = "이번 주 예측 수와 정답률을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/weekly-summary")
    public ResponseEntity<BaseResponse<WeeklySummaryResponse>> getWeeklySummary(Authentication authentication) {
        Long userId = requireUserId(authentication);
        return ResponseEntity.ok(BaseResponse.success(predictionService.getWeeklySummary(userId)));
    }

    private Long requireUserId(Authentication authentication) {
        Long userId = extractPrincipalUserId(authentication);
        if (userId == null) {
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED_ACCESS);
        }
        return userId;
    }

    private Long extractPrincipalUserId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long id) {
            return id;
        }
        if (principal instanceof Integer id) {
            return id.longValue();
        }
        return null;
    }
}
