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
import com.whyitrose.domain.stock.StockListSnapshot;
import com.whyitrose.domain.stock.StockListSnapshotRepository;
import com.whyitrose.domain.stock.StockMarket;
import com.whyitrose.domain.stock.StockPrice;
import com.whyitrose.domain.stock.StockPricePeriod;
import com.whyitrose.domain.stock.StockPriceRepository;
import com.whyitrose.domain.stock.StockRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StockService {

    private final StockRepository stockRepository;
    private final StockPriceRepository stockPriceRepository;
    private final StockListSnapshotRepository stockListSnapshotRepository;

    public StockListResponse getStocks(String market, String sort, String period, String cursor, Integer size) {
        int pageSize = Math.max(1, Math.min(size == null ? 20 : size, 100));
        String sortKey = normalizeSort(sort);
        String periodKey = normalizePeriod(period);

        StockListCursor parsedCursor = decodeCursor(cursor, sortKey);
        StockMarket marketFilter = resolveMarket(market);
        int startRank = parsedCursor == null ? 1 : parsedCursor.nextRank();
        Pageable pageable = PageRequest.of(0, pageSize + 1);
        List<StockListSnapshot> rows = fetchSnapshots(sortKey, periodKey, marketFilter, parsedCursor, pageable);
        boolean hasNext = rows.size() > pageSize;
        if (hasNext) {
            rows = rows.subList(0, pageSize);
        }

        List<StockListItem> ranked = new ArrayList<>();
        int rank = startRank;
        for (StockListSnapshot row : rows) {
            Stock stock = row.getStock();
            ChangeDirection direction = row.getPriceChange() > 0
                    ? ChangeDirection.UP
                    : (row.getPriceChange() < 0 ? ChangeDirection.DOWN : ChangeDirection.FLAT);
            ranked.add(new StockListItem(
                    rank++,
                    stock.getId(),
                    stock.getTicker(),
                    stock.getName(),
                    stock.getMarket().name(),
                    stock.getLogoUrl(),
                    row.getCurrentPrice(),
                    row.getPriceChange(),
                    row.getChangeRate(),
                    direction,
                    row.getTradingAmount(),
                    row.getTradingVolume(),
                    false,
                    null,
                    false
            ));
        }

        String nextCursor = hasNext && !rows.isEmpty()
                ? encodeCursor(buildCursor(sortKey, rows.get(rows.size() - 1), rank))
                : null;
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
        return latestSnapshot(stockId, mappedPeriod, tradingDays);
    }

    private PriceSnapshot latestSnapshot(Long stockId, StockPricePeriod mappedPeriod, int tradingDays) {
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

    private String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return "TRADING_AMOUNT";
        }
        String key = sort.toUpperCase(Locale.ROOT);
        return switch (key) {
            case "TRADING_VOLUME", "SURGE", "DROP", "TRADING_AMOUNT" -> key;
            default -> "TRADING_AMOUNT";
        };
    }

    private StockMarket resolveMarket(String market) {
        if (market == null || market.isBlank() || "ALL".equalsIgnoreCase(market)) {
            return null;
        }
        try {
            return StockMarket.valueOf(market.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private List<StockListSnapshot> fetchSnapshots(
            String sortKey,
            String periodKey,
            StockMarket marketFilter,
            StockListCursor cursor,
            Pageable pageable
    ) {
        if (cursor == null) {
            return switch (sortKey) {
                case "TRADING_VOLUME" -> stockListSnapshotRepository.findFirstByTradingVolume(periodKey, marketFilter, pageable);
                case "SURGE" -> stockListSnapshotRepository.findFirstBySurge(periodKey, marketFilter, pageable);
                case "DROP" -> stockListSnapshotRepository.findFirstByDrop(periodKey, marketFilter, pageable);
                default -> stockListSnapshotRepository.findFirstByTradingAmount(periodKey, marketFilter, pageable);
            };
        }
        return switch (sortKey) {
            case "TRADING_VOLUME" -> stockListSnapshotRepository.findNextByTradingVolume(
                    periodKey,
                    marketFilter,
                    Long.parseLong(cursor.sortValue()),
                    cursor.stockId(),
                    pageable
            );
            case "SURGE" -> stockListSnapshotRepository.findNextBySurge(
                    periodKey,
                    marketFilter,
                    Double.parseDouble(cursor.sortValue()),
                    cursor.stockId(),
                    pageable
            );
            case "DROP" -> stockListSnapshotRepository.findNextByDrop(
                    periodKey,
                    marketFilter,
                    Double.parseDouble(cursor.sortValue()),
                    cursor.stockId(),
                    pageable
            );
            default -> stockListSnapshotRepository.findNextByTradingAmount(
                    periodKey,
                    marketFilter,
                    Long.parseLong(cursor.sortValue()),
                    cursor.stockId(),
                    pageable
            );
        };
    }

    public synchronized void refreshAllStockListSnapshots() {
        List<String> periodKeys = List.of("REALTIME", "1D", "1W", "1M", "3M", "6M", "1Y");
        for (String periodKey : periodKeys) {
            recomputeSnapshots(periodKey, resolvePricePeriod(periodKey));
        }
    }

    private void recomputeSnapshots(String periodKey, StockPricePeriod mappedPeriod) {
        List<Stock> stocks = stockRepository.findByStatusOrderByIdAsc(Status.ACTIVE);
        if (stocks.isEmpty()) {
            return;
        }
        List<Long> stockIds = stocks.stream().map(Stock::getId).toList();
        Map<Long, StockListSnapshot> existingByStockId = stockListSnapshotRepository
                .findByPeriodKeyAndStockIdIn(periodKey, stockIds)
                .stream()
                .collect(java.util.stream.Collectors.toMap(s -> s.getStock().getId(), s -> s));

        List<StockListSnapshot> upserts = new ArrayList<>(stocks.size());
        int tradingDays = resolveTradingDays(periodKey);
        for (Stock stock : stocks) {
            PriceSnapshot snapshot = latestSnapshot(stock.getId(), mappedPeriod, tradingDays);
            StockListSnapshot existing = existingByStockId.get(stock.getId());
            if (existing == null) {
                upserts.add(StockListSnapshot.create(
                        stock,
                        periodKey,
                        snapshot.currentPrice,
                        snapshot.priceChange,
                        snapshot.changeRate,
                        snapshot.tradingAmount,
                        snapshot.tradingVolume
                ));
            } else {
                existing.apply(
                        snapshot.currentPrice,
                        snapshot.priceChange,
                        snapshot.changeRate,
                        snapshot.tradingAmount,
                        snapshot.tradingVolume
                );
                upserts.add(existing);
            }
        }
        stockListSnapshotRepository.saveAll(upserts);
    }

    private StockListCursor buildCursor(String sortKey, StockListSnapshot row, int nextRank) {
        Long stockId = row.getStock().getId();
        return switch (sortKey) {
            case "TRADING_VOLUME" -> new StockListCursor(sortKey, String.valueOf(row.getTradingVolume()), stockId, nextRank);
            case "SURGE", "DROP" -> new StockListCursor(sortKey, String.valueOf(row.getChangeRate()), stockId, nextRank);
            default -> new StockListCursor(sortKey, String.valueOf(row.getTradingAmount()), stockId, nextRank);
        };
    }

    private String encodeCursor(StockListCursor cursor) {
        String raw = cursor.sortKey() + "|" + cursor.sortValue() + "|" + cursor.stockId() + "|" + cursor.nextRank();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private StockListCursor decodeCursor(String cursor, String sortKey) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|");
            if (parts.length != 4) {
                return null;
            }
            if (!sortKey.equals(parts[0])) {
                return null;
            }
            return new StockListCursor(parts[0], parts[1], Long.parseLong(parts[2]), Integer.parseInt(parts[3]));
        } catch (Exception ignored) {
            return null;
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

    private record StockListCursor(
            String sortKey,
            String sortValue,
            long stockId,
            int nextRank
    ) {}
}
