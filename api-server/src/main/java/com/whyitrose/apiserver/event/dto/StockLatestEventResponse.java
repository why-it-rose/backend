package com.whyitrose.apiserver.event.dto;

import com.whyitrose.domain.event.Event;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "종목별 최신 이벤트 응답")
public record StockLatestEventResponse(
        @Schema(description = "종목 ID", example = "10")
        Long stockId,

        @Schema(description = "이벤트 ID", example = "1")
        Long eventId,

        @Schema(description = "이벤트 유형", example = "SURGE", allowableValues = {"SURGE", "DROP"})
        String eventType,

        @Schema(description = "이벤트 시작일", example = "2024-03-16")
        LocalDate startDate,

        @Schema(description = "이벤트 종료일", example = "2024-03-16")
        LocalDate endDate,

        @Schema(description = "등락률 (%)", example = "12.35")
        BigDecimal changePct
) {
    public static StockLatestEventResponse from(Event event) {
        return new StockLatestEventResponse(
                event.getStock().getId(),
                event.getId(),
                event.getEventType().name(),
                event.getStartDate(),
                event.getEndDate(),
                event.getChangePct()
        );
    }
}
