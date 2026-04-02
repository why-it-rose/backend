package com.whyitrose.apiserver.stock.kis;

public record KisFinancialRatio(
        String baseDate,
        double revenueGrowthRate,
        double operatingProfitGrowthRate,
        double netProfitGrowthRate
) {}
