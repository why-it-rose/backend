package com.whyitrose.apiserver.stock.kis;

public record KisIncomeStatement(
        String baseDate,
        long revenue,
        long previousRevenue,
        long operatingProfit,
        long netProfit
) {}
