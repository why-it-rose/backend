package com.whyitrose.apiserver.me.dto;

public record WeeklySummaryResponse(
        long weeklyTotal,
        long weeklyCorrect,
        Double weeklyAccuracy
) {}
