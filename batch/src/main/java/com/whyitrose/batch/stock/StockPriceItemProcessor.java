package com.whyitrose.batch.stock;

import com.fasterxml.jackson.databind.JsonNode;
import com.whyitrose.batch.ls.LsMarketDataClient;
import com.whyitrose.domain.stock.Stock;
import com.whyitrose.domain.stock.StockPricePeriod;
import com.whyitrose.domain.stock.StockPriceRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockPriceItemProcessor implements ItemProcessor<Stock, StockPriceBatch> {

    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    // DAILY→WEEKLY→MONTHLY→YEARLY 순 고정 (전체 로드)
    private static final List<StockPricePeriod> ALL_PERIODS = List.of(
            StockPricePeriod.DAILY,
            StockPricePeriod.WEEKLY,
            StockPricePeriod.MONTHLY,
            StockPricePeriod.YEARLY);

    // 데일리 업데이트: YEARLY 제외 (연봉은 자주 바뀌지 않음)
    private static final List<StockPricePeriod> DAILY_PERIODS = List.of(
            StockPricePeriod.DAILY,
            StockPricePeriod.WEEKLY,
            StockPricePeriod.MONTHLY);

    private static final List<String> ALL_GUBUNS = List.of("2", "3", "4", "5");
    private static final List<String> DAILY_GUBUNS = List.of("2", "3", "4");

    private final LsMarketDataClient lsMarketDataClient;
    private final StockPriceRepository stockPriceRepository;

    /** 0 = 전체 이력 로드, N = 최근 N일 기준 fallback (DB에 데이터 없을 때만 사용) */
    @Value("${stock.price.days-back:0}")
    private int daysBack;

    @Override
    public StockPriceBatch process(Stock stock) throws Exception {
        List<StockPriceBatch.PriceRow> rows = new ArrayList<>();

        List<StockPricePeriod> periods = daysBack > 0 ? DAILY_PERIODS : ALL_PERIODS;
        List<String> gubuns = daysBack > 0 ? DAILY_GUBUNS : ALL_GUBUNS;
        String sdate = daysBack > 0 ? resolveSdate(stock) : "";

        for (int i = 0; i < periods.size(); i++) {
            StockPricePeriod period = periods.get(i);
            String gubun = gubuns.get(i);

            List<JsonNode> apiRows = lsMarketDataClient.fetchAllChartRows(stock.getTicker(), gubun, "K", sdate);
            for (JsonNode row : apiRows) {
                LocalDate tradingDate = parseDate(text(row, "date"));
                if (tradingDate == null) {
                    continue;
                }
                rows.add(new StockPriceBatch.PriceRow(
                        tradingDate, period,
                        parseInt(text(row, "open")),
                        parseInt(text(row, "close")),
                        parseInt(text(row, "high")),
                        parseInt(text(row, "low")),
                        parseLong(text(row, "jdiff_vol"))));
            }
            log.info("t8451 조회: ticker={}, period={}, rows={}", stock.getTicker(), period, apiRows.size());
        }

        return new StockPriceBatch(stock, rows);
    }

    /**
     * 동적 sdate 계산 — 공백 기간 자동 보정
     * DB에 저장된 마지막 DAILY 가격 날짜 + 1일을 시작일로 사용.
     * 데이터가 전혀 없으면 days-back 기준으로 fallback.
     */
    private String resolveSdate(Stock stock) {
        return stockPriceRepository
                .findTopByStockIdAndPeriodOrderByTradingDateDesc(stock.getId(), StockPricePeriod.DAILY)
                .map(lastPrice -> {
                    LocalDate from = lastPrice.getTradingDate().plusDays(1);
                    log.info("동적 sdate: ticker={}, lastDate={}, from={}", stock.getTicker(), lastPrice.getTradingDate(), from);
                    return from.format(BASIC_DATE);
                })
                .orElseGet(() -> {
                    LocalDate fallback = LocalDate.now().minusDays(daysBack);
                    log.info("fallback sdate: ticker={}, from={}", stock.getTicker(), fallback);
                    return fallback.format(BASIC_DATE);
                });
    }

    private static LocalDate parseDate(String yyyyMMdd) {
        if (yyyyMMdd == null || yyyyMMdd.isBlank() || yyyyMMdd.length() != 8) {
            return null;
        }
        return LocalDate.parse(yyyyMMdd, BASIC_DATE);
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return "";
        }
        String value = node.get(field).asText();
        return value == null ? "" : value.trim();
    }

    private static int parseInt(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.replace(",", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value.replace(",", ""));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}