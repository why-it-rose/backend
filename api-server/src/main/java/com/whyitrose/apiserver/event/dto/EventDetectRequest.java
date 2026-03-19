package com.whyitrose.apiserver.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "이벤트 탐지 요청")
public record EventDetectRequest(
        @Schema(description = "탐지할 날짜", example = "2024-03-16")
        @NotNull(message = "탐지 날짜를 입력해주세요.") LocalDate targetDate
) {
}
