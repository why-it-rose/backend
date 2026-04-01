package com.whyitrose.apiserver.prediction.dto;

import java.time.LocalDate;
import java.util.List;

public record PredictionGroupResponse(
        LocalDate digestDate,
        List<PredictionResponse> predictions
) {
    public static PredictionGroupResponse of(LocalDate date, List<PredictionResponse> predictions) {
        return new PredictionGroupResponse(date, predictions);
    }
}
