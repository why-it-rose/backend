package com.whyitrose.apiserver.stock.kis;

public record KisInvestorTrading(
        String baseDate,
        long foreignNetBuyAmount,
        long institutionNetBuyAmount,
        long individualNetBuyAmount
) {}
