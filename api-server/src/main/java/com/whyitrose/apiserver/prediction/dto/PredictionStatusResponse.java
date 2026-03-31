package com.whyitrose.apiserver.prediction.dto;

import com.whyitrose.domain.prediction.PredictionDirection;

public record PredictionStatusResponse(
        Long id,
        PredictionDirection direction,
        String reason,
        boolean canPredict
) {}