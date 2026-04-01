package com.whyitrose.apiserver.prediction.controller;

import com.whyitrose.apiserver.prediction.dto.PredictionPageResponse;
import com.whyitrose.apiserver.prediction.dto.PredictionRequest;
import com.whyitrose.apiserver.prediction.dto.PredictionResponse;
import com.whyitrose.apiserver.prediction.dto.PredictionStatusResponse;
import com.whyitrose.apiserver.prediction.service.PredictionService;
import com.whyitrose.core.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "prediction-controller", description = "예측·복기 API")
@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionService predictionService;

    @Operation(summary = "예측 등록/수정", description = "오늘의 학습 종목에 대한 예측을 등록하거나 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "예측 등록/수정 성공"),
            @ApiResponse(responseCode = "400", description = "오늘의 학습이 아닌 경우"),
            @ApiResponse(responseCode = "404", description = "다이제스트 또는 종목 없음")
    })
    @PostMapping
    public ResponseEntity<BaseResponse<PredictionResponse>> upsert(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid PredictionRequest request
    ) {
        return ResponseEntity.ok(BaseResponse.success(predictionService.upsert(userId, request)));
    }

    @Operation(summary = "예측·복기 목록 조회", description = "날짜별로 그룹핑된 예측 목록을 커서 기반으로 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public ResponseEntity<BaseResponse<PredictionPageResponse>> getMyPredictions(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(BaseResponse.success(predictionService.getMyPredictionsPaged(userId, cursor, size)));
    }

    @Operation(summary = "종목 예측 상태 조회", description = "특정 학습의 종목에 대한 기존 예측 상태를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "다이제스트 없음")
    })
    @GetMapping("/digest/{digestId}/stocks/{stockId}")
    public ResponseEntity<BaseResponse<PredictionStatusResponse>> getStatus(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long digestId,
            @PathVariable Long stockId
    ) {
        return ResponseEntity.ok(BaseResponse.success(predictionService.getStatus(userId, digestId, stockId)));
    }
}
