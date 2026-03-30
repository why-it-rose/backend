package com.whyitrose.apiserver.scrap.dto;

import com.whyitrose.domain.scrap.Scrap;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "스크랩 응답")
public record ScrapResponse(

        @Schema(description = "스크랩 ID", example = "1")
        Long scrapId,

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

        @Schema(description = "등락률 (%)", example = "19.47")
        BigDecimal changePct,

        @Schema(description = "이벤트 시작일", example = "2025-03-22")
        LocalDate startDate,

        @Schema(description = "이벤트 종료일", example = "2025-03-22")
        LocalDate endDate,

        @Schema(description = "스크랩 일시", example = "2026-03-30T10:00:00")
        LocalDateTime scrapedAt
) {
    public static ScrapResponse from(Scrap scrap) {
        return new ScrapResponse(
                scrap.getId(),
                scrap.getEvent().getId(),
                scrap.getEvent().getStock().getId(),
                scrap.getEvent().getStock().getName(),
                scrap.getEvent().getStock().getTicker(),
                scrap.getEvent().getEventType().name(),
                scrap.getEvent().getChangePct(),
                scrap.getEvent().getStartDate(),
                scrap.getEvent().getEndDate(),
                scrap.getCreatedAt()
        );
    }
}
