package com.whyitrose.apiserver.me.dto;

public record MyPageStatsResponse(
        long totalPredictions,
        long correctPredictions,
        Double predictionAccuracy,
        long totalScraps
) {}
