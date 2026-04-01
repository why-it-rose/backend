package com.whyitrose.apiserver.prediction.dto;

import com.whyitrose.domain.prediction.Prediction;
import com.whyitrose.domain.prediction.PredictionDirection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PredictionResponse(
        Long id,
        Long digestId,
        Long stockId,
        String stockName,
        String stockTicker,
        String stockLogoUrl,
        PredictionDirection direction,
        String reason,
        BigDecimal actualChangePct,
        Boolean isCorrect,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt
) {
    public static PredictionResponse from(Prediction prediction) {
        return new PredictionResponse(
                prediction.getId(),
                prediction.getDigest().getId(),
                prediction.getStock().getId(),
                prediction.getStock().getName(),
                prediction.getStock().getTicker(),
                prediction.getStock().getLogoUrl(),
                prediction.getDirection(),
                prediction.getReason(),
                prediction.getActualChangePct(),
                calculateIsCorrect(prediction.getDirection(), prediction.getActualChangePct()),
                prediction.getCreatedAt(),
                prediction.getReviewedAt()
        );
    }

    private static Boolean calculateIsCorrect(PredictionDirection direction, BigDecimal actualChangePct) {
        if (actualChangePct == null) {
            return null;
        }
        return switch (direction) {
            case UP -> actualChangePct.compareTo(BigDecimal.ZERO) > 0;
            case DOWN -> actualChangePct.compareTo(BigDecimal.ZERO) < 0;
            case SIDEWAYS -> actualChangePct.abs().compareTo(new BigDecimal("2.00")) <= 0;
        };
    }
}
