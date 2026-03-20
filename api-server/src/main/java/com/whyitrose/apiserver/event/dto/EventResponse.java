package com.whyitrose.apiserver.event.dto;

import com.whyitrose.domain.event.Event;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "이벤트 목록 응답")
public record EventResponse(
        @Schema(description = "이벤트 ID", example = "1")
        Long eventId,

        @Schema(description = "종목 ID", example = "10")
        Long stockId,

        @Schema(description = "종목명", example = "삼성전자")
        String stockName,

        @Schema(description = "종목코드", example = "005930")
        String ticker,

        @Schema(description = "이벤트 유형", example = "SURGE", allowableValues = {"SURGE", "DROP"})
        String eventType,

        @Schema(description = "이벤트 시작일", example = "2024-03-16")
        LocalDate startDate,

        @Schema(description = "이벤트 종료일", example = "2024-03-16")
        LocalDate endDate,

        @Schema(description = "등락률 (%)", example = "12.35")
        BigDecimal changePct,

        @Schema(description = "시작일 전일 종가 (원)", example = "70000")
        int priceBefore,

        @Schema(description = "종료일 종가 (원)", example = "78650")
        int priceAfter
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getStock().getId(),
                event.getStock().getName(),
                event.getStock().getTicker(),
                event.getEventType().name(),
                event.getStartDate(),
                event.getEndDate(),
                event.getChangePct(),
                event.getPriceBefore(),
                event.getPriceAfter()
        );
    }
}
