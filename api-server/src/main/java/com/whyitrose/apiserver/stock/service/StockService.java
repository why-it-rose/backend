package com.whyitrose.apiserver.stock.service;

import com.whyitrose.apiserver.stock.dto.StockDtos.CandleDto;
import com.whyitrose.apiserver.stock.dto.StockDtos.ChangeDirection;
import com.whyitrose.apiserver.stock.dto.StockDtos.StockDetailResponse;
import com.whyitrose.apiserver.stock.dto.StockDtos.StockListItem;
import com.whyitrose.apiserver.stock.dto.StockDtos.StockListResponse;
import com.whyitrose.apiserver.stock.dto.StockDtos.StockPricesResponse;
import com.whyitrose.apiserver.stock.dto.StockDtos.StockSearchItem;
import com.whyitrose.apiserver.stock.dto.StockDtos.StockSearchResponse;
import com.whyitrose.apiserver.stock.dto.StockDtos.TodayOhlcv;
import com.whyitrose.apiserver.stock.exception.StockErrorCode;
import com.whyitrose.core.exception.BaseException;
import com.whyitrose.domain.common.Status;
import com.whyitrose.domain.stock.Stock;
import com.whyitrose.domain.stock.StockPrice;
import com.whyitrose.domain.stock.StockPricePeriod;
import com.whyitrose.domain.stock.StockPriceRepository;
import com.whyitrose.domain.stock.StockRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private final StockRepository stockRepository;
    private final StockPriceRepository stockPriceRepository;

    public StockListResponse getStocks(String market, String sort, String period, String cursor, Integer size) {
        int pageSize = Math.max(1, Math.min(size == null ? 20 : size, 100));
        int offset = parseCursor(cursor);
        List<Stock> stocks = stockRepository.findByStatusOrderByIdAsc(Status.ACTIVE);
        if (market != null && !"ALL".equalsIgnoreCase(market)) {
            stocks = stocks.stream()
                    .filter(s -> s.getMarket().name().equalsIgnoreCase(market))
                    .toList();
        }

        List<StockListItem> allItems = new ArrayList<>();
        for (Stock stock : stocks) {
            PriceSnapshot snapshot = latestSnapshot(stock.getId(), period);
            allItems.add(new StockListItem(
                    0,
                    stock.getId(),
                    stock.getTicker(),
                    stock.getName(),
                    stock.getMarket().name(),
                    stock.getLogoUrl(),
                    snapshot.currentPrice,
                    snapshot.priceChange,
                    snapshot.changeRate,
                    snapshot.direction,
                    snapshot.tradingAmount,
                    snapshot.tradingVolume,
                    false,
                    null,
                    false
            ));
        }

        allItems.sort(stockComparator(sort));
        if (offset < 0 || offset > allItems.size()) {
            offset = 0;
        }
        int endExclusive = Math.min(allItems.size(), offset + pageSize);
        List<StockListItem> content = allItems.subList(offset, endExclusive);
        boolean hasNext = endExclusive < allItems.size();
        List<StockListItem> ranked = new ArrayList<>();
        int rank = offset + 1;
        for (StockListItem item : content) {
            ranked.add(new StockListItem(
                    rank++,
                    item.stockId(),
                    item.ticker(),
                    item.name(),
                    item.market(),
                    item.logoUrl(),
                    item.currentPrice(),
                    item.priceChange(),
                    item.changeRate(),
                    item.changeDirection(),
                    item.tradingAmount(),
                    item.tradingVolume(),
                    item.hasEvent(),
                    item.eventType(),
                    item.isInterested()
            ));
        }

        String nextCursor = hasNext ? String.valueOf(endExclusive) : null;
        return new StockListResponse(nextCursor, hasNext, ranked.size(), ranked);
    }

    public StockSearchResponse searchStocks(String query, Integer limit) {
        if (query == null || query.trim().isBlank()) {
            throw new BaseException(StockErrorCode.STOCK_004);
        }
        String q = query.trim();
        int size = Math.max(1, Math.min(limit == null ? 10 : limit, 20));
        List<Stock> tickerMatches = stockRepository.findByStatusAndTickerContainingIgnoreCaseOrderByIdAsc(
                Status.ACTIVE, q, PageRequest.of(0, size));
        List<Stock> nameMatches = stockRepository.findByStatusAndNameContainingIgnoreCaseOrderByIdAsc(
                Status.ACTIVE, q, PageRequest.of(0, size));

        Map<Long, Stock> dedup = new LinkedHashMap<>();
        for (Stock stock : tickerMatches) {
            dedup.put(stock.getId(), stock);
        }
        for (Stock stock : nameMatches) {
            dedup.put(stock.getId(), stock);
        }

        List<StockSearchItem> items = dedup.values().stream()
                .limit(size)
                .map(this::toSearchItem)
                .toList();

        return new StockSearchResponse(q, items.size(), items);
    }

    public StockSearchItem toSearchItem(Stock stock) {
        PriceSnapshot snapshot = latestSnapshot(stock.getId(), "1D");
        return new StockSearchItem(
                stock.getId(),
                stock.getTicker(),
                stock.getName(),
                stock.getMarket().name(),
                stock.getLogoUrl(),
                snapshot.currentPrice,
                snapshot.changeRate,
                snapshot.direction
        );
    }

    public StockDetailResponse getStockDetail(Long stockId) {
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new BaseException(StockErrorCode.STOCK_001));
        PriceSnapshot snapshot = latestSnapshot(stockId, "1D");
        StockPrice latestDaily = stockPriceRepository.findTopByStockIdAndPeriodOrderByTradingDateDesc(
                        stockId, StockPricePeriod.DAILY)
                .orElse(null);

        TodayOhlcv ohlcv = new TodayOhlcv(
                latestDaily == null ? 0 : latestDaily.getOpenPrice(),
                latestDaily == null ? 0 : latestDaily.getHighPrice(),
                latestDaily == null ? 0 : latestDaily.getLowPrice(),
                latestDaily == null ? 0 : latestDaily.getVolume()
        );

        return new StockDetailResponse(
                stock.getId(),
                stock.getTicker(),
                stock.getName(),
                stock.getMarket().name(),
                stock.getSector(),
                stock.getLogoUrl(),
                snapshot.currentPrice,
                snapshot.priceChange,
                snapshot.changeRate,
                snapshot.direction,
                ohlcv,
                false
        );
    }

    public StockPricesResponse getStockPrices(Long stockId, String period) {
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new BaseException(StockErrorCode.STOCK_001));
        String key = normalizePeriod(period);
        StockPricePeriod mappedPeriod = resolvePricePeriod(key);
        List<StockPrice> prices = stockPriceRepository.findByStockIdAndPeriodOrderByTradingDateAsc(
                stock.getId(), mappedPeriod);
        if (prices.isEmpty()) {
            throw new BaseException(StockErrorCode.STOCK_002);
        }
        List<CandleDto> candles = prices.stream()
                .map(price -> new CandleDto(
                        price.getTradingDate(),
                        price.getOpenPrice(),
                        price.getClosePrice(),
                        price.getHighPrice(),
                        price.getLowPrice(),
                        price.getVolume()
                ))
                .toList();

        return new StockPricesResponse(stock.getId(), key, candles, List.of(), List.of());
    }

    private PriceSnapshot latestSnapshot(Long stockId, String period) {
        String key = normalizePeriod(period);
        StockPricePeriod mappedPeriod = resolvePricePeriod(key);
        int tradingDays = resolveTradingDays(key);
        List<StockPrice> desc = stockPriceRepository.findByStockIdAndPeriodOrderByTradingDateDesc(
                stockId, mappedPeriod, PageRequest.of(0, Math.max(2, tradingDays + 1)));
        if (desc.isEmpty()) {
            return new PriceSnapshot(0, 0, 0.0, ChangeDirection.FLAT, 0, 0);
        }
        StockPrice current = desc.get(0);
        StockPrice previous = desc.size() > tradingDays ? desc.get(tradingDays) : desc.get(desc.size() - 1);

        long currentPrice = current.getClosePrice();
        long previousPrice = previous == null ? currentPrice : previous.getClosePrice();
        long change = currentPrice - previousPrice;
        double rate = previousPrice == 0 ? 0.0 : ((double) change / previousPrice) * 100.0;
        ChangeDirection direction = change > 0 ? ChangeDirection.UP : (change < 0 ? ChangeDirection.DOWN : ChangeDirection.FLAT);
        int windowSize = Math.max(1, Math.min(tradingDays, desc.size()));
        long tradingVolume = 0L;
        long tradingAmount = 0L;
        for (int i = 0; i < windowSize; i++) {
            StockPrice price = desc.get(i);
            tradingVolume += price.getVolume();
            tradingAmount += (long) price.getClosePrice() * price.getVolume();
        }

        return new PriceSnapshot(currentPrice, change, round2(rate), direction, tradingAmount, tradingVolume);
    }

    private Comparator<StockListItem> stockComparator(String sort) {
        String key = sort == null ? "TRADING_AMOUNT" : sort.toUpperCase(Locale.ROOT);
        return switch (key) {
            case "TRADING_VOLUME" -> Comparator.comparingLong(StockListItem::tradingVolume).reversed()
                    .thenComparingLong(StockListItem::stockId);
            case "SURGE" -> Comparator.comparingDouble(StockListItem::changeRate).reversed()
                    .thenComparingLong(StockListItem::stockId);
            case "DROP" -> Comparator.comparingDouble(StockListItem::changeRate)
                    .thenComparingLong(StockListItem::stockId);
            default -> Comparator.comparingLong(StockListItem::tradingAmount).reversed()
                    .thenComparingLong(StockListItem::stockId);
        };
    }

    private int parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(cursor);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String normalizePeriod(String period) {
        if (period == null || period.isBlank()) {
            return "6M";
        }
        String key = period.toUpperCase(Locale.ROOT);
        return switch (key) {
            case "REALTIME", "1D", "1W", "1M", "3M", "6M", "1Y" -> key;
            default -> "6M";
        };
    }

    private int resolveTradingDays(String period) {
        String key = normalizePeriod(period);
        return switch (key) {
            case "REALTIME", "1D" -> 1;
            case "1W", "1M", "1Y" -> 1;
            case "3M" -> 60;
            case "6M" -> 120;
            default -> 120;
        };
    }

    private StockPricePeriod resolvePricePeriod(String period) {
        String key = normalizePeriod(period);
        return switch (key) {
            case "1W" -> StockPricePeriod.WEEKLY;
            case "1M" -> StockPricePeriod.MONTHLY;
            case "1Y" -> StockPricePeriod.YEARLY;
            default -> StockPricePeriod.DAILY;
        };
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record PriceSnapshot(
            long currentPrice,
            long priceChange,
            double changeRate,
            ChangeDirection direction,
            long tradingAmount,
            long tradingVolume
    ) {}
}
