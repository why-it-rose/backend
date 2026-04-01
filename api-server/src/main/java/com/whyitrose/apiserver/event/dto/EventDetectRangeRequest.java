package com.whyitrose.apiserver.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "기간 이벤트 탐지 요청")
public record EventDetectRangeRequest(
        @Schema(description = "탐지 시작 날짜 (포함)", example = "2025-01-02")
        @NotNull(message = "시작 날짜를 입력해주세요.") LocalDate from,

        @Schema(description = "탐지 종료 날짜 (포함)", example = "2025-03-21")
        @NotNull(message = "종료 날짜를 입력해주세요.") LocalDate to
) {
}
