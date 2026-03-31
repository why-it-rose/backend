package com.whyitrose.apiserver.prediction.dto;

import com.whyitrose.domain.prediction.PredictionDirection;
import jakarta.validation.constraints.NotNull;

public record PredictionRequest(
        @NotNull Long digestId,
        @NotNull Long stockId,
        @NotNull PredictionDirection direction,
        String reason
) {}
