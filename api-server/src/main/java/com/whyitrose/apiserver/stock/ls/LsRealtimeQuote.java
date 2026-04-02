package com.whyitrose.apiserver.stock.ls;

public record LsRealtimeQuote(
        String name,
        long currentPrice,
        long priceChange,
        double changeRate
) {
}
