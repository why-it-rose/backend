package com.whyitrose.apiserver.scrap.dto;

import com.whyitrose.domain.scrap.Scrap;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MyScrapSearchItemResponse(
        Long eventId,
        String stockName,
        String ticker,
        String eventType,
        LocalDate startDate,
        BigDecimal changePct,
        boolean isScrapped,
        String logoUrl
) {
    public static MyScrapSearchItemResponse from(Scrap scrap) {
        return new MyScrapSearchItemResponse(
                scrap.getEvent().getId(),
                scrap.getEvent().getStock().getName(),
                scrap.getEvent().getStock().getTicker(),
                scrap.getEvent().getEventType().name(),
                scrap.getEvent().getStartDate(),
                scrap.getEvent().getChangePct(),
                true,
                scrap.getEvent().getStock().getLogoUrl()
                );
    }
}
