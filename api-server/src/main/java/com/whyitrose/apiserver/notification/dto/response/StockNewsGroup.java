package com.whyitrose.apiserver.notification.dto.response;

import java.util.List;

public record StockNewsGroup(
        Long stockId,
        String stockName,
        String ticker,
        int newsCount,
        List<NotificationNewsItem> newsList
) {
}
