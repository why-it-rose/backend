package com.whyitrose.batch.stock;

import com.fasterxml.jackson.databind.JsonNode;
import com.whyitrose.batch.ls.LsMarketDataClient;
import com.whyitrose.domain.common.Status;
import com.whyitrose.domain.stock.Stock;
import com.whyitrose.domain.stock.StockPrice;
import com.whyitrose.domain.stock.StockPricePeriod;
import com.whyitrose.domain.stock.StockPriceRepository;
import com.whyitrose.domain.stock.StockRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockPriceLoadService {

    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final LsMarketDataClient lsMarketDataClient;
    private final StockRepository stockRepository;
    private final StockPriceRepository stockPriceRepository;
    private final TransactionTemplate transactionTemplate;
    @Value("${stock.price.single-ticker:}")
    private String singleTicker;
    @Value("${stock.price.start-id:0}")
    private long startId;
    @Value("${stock.price.end-id:9223372036854775807}")
    private long endId;

    public void loadAllPeriodsForActiveStocks() throws InterruptedException {
        List<Stock> stocks = stockRepository.findByStatusOrderByIdAsc(Status.ACTIVE);
        long fromId = Math.max(0L, startId);
        long toId = Math.max(fromId, endId);
        stocks = stocks.stream()
                .filter(stock -> stock.getId() != null
                        && stock.getId() >= fromId
                        && stock.getId() <= toId)
                .collect(Collectors.toList());
        log.info("ID 범위 필터 적용: {} ~ {}, 대상 종목 {}개", fromId, toId, stocks.size());
        String tickerFilter = singleTicker == null ? "" : singleTicker.trim().toUpperCase(Locale.ROOT);
        if (!tickerFilter.isBlank()) {
            stocks = stocks.stream()
                    .filter(stock -> tickerFilter.equalsIgnoreCase(stock.getTicker()))
                    .collect(Collectors.toList());
            log.info("단일 종목 모드 활성화: ticker={}, 대상 종목 {}개", tickerFilter, stocks.size());
        }
        log.info("주가 차트(t8451) 적재 시작: 대상 종목 {}개", stocks.size());

        int index = 1;
        for (Stock stock : stocks) {
            log.info("종목 적재 시작: [{}/{}] id={}, ticker={}", index, stocks.size(), stock.getId(), stock.getTicker());
            loadOneStockAllPeriods(stock);
            log.info("종목 적재 완료: [{}/{}] id={}, ticker={}", index, stocks.size(), stock.getId(), stock.getTicker());
            index++;
        }
        log.info("주가 차트(t8451) 적재 완료");
    }

    private void loadOneStockAllPeriods(Stock stock) throws InterruptedException {
        transactionTemplate.executeWithoutResult(status -> {
            try {
                loadOneStock(stock, StockPricePeriod.DAILY, "2");
                loadOneStock(stock, StockPricePeriod.WEEKLY, "3");
                loadOneStock(stock, StockPricePeriod.MONTHLY, "4");
                loadOneStock(stock, StockPricePeriod.YEARLY, "5");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("종목 적재 중 인터럽트 발생: ticker=" + stock.getTicker(), e);
            }
        });
    }

    private void loadOneStock(Stock stock, StockPricePeriod period, String gubun) throws InterruptedException {
        List<JsonNode> rows = lsMarketDataClient.fetchAllChartRows(stock.getTicker(), gubun, toExchgubun(stock));
        int saved = 0;
        for (JsonNode row : rows) {
            LocalDate tradingDate = parseDate(text(row, "date"));
            if (tradingDate == null) {
                continue;
            }
            int open = parseInt(text(row, "open"));
            int high = parseInt(text(row, "high"));
            int low = parseInt(text(row, "low"));
            int close = parseInt(text(row, "close"));
            long volume = parseLong(text(row, "jdiff_vol"));

            upsertPrice(stock, tradingDate, period, open, close, high, low, volume);
            saved++;
        }
        log.info("t8451 적재: ticker={}, period={}, rows={}", stock.getTicker(), period, saved);
    }

    private void upsertPrice(
            Stock stock,
            LocalDate tradeDate,
            StockPricePeriod period,
            int open,
            int close,
            int high,
            int low,
            long volume) {
        stockPriceRepository.findByStockIdAndTradingDateAndPeriod(stock.getId(), tradeDate, period)
                .map(existing -> {
                    existing.applyPrice(open, close, high, low, volume);
                    return stockPriceRepository.save(existing);
                })
                .orElseGet(() -> stockPriceRepository.save(
                        StockPrice.create(stock, tradeDate, period, open, close, high, low, volume)));
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return "";
        }
        String value = node.get(field).asText();
        return value == null ? "" : value.trim();
    }

    private static LocalDate parseDate(String yyyyMMdd) {
        if (yyyyMMdd == null || yyyyMMdd.isBlank() || yyyyMMdd.length() != 8) {
            return null;
        }
        return LocalDate.parse(yyyyMMdd, BASIC_DATE);
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

    private static String toExchgubun(Stock stock) {
        // KRX 기준: 코스피/코스닥 모두 "K"로 조회
        return "K";
    }
}
