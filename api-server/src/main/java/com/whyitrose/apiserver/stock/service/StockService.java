package com.whyitrose.apiserver.stock.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whyitrose.apiserver.stock.dto.StockDtos.CandleDto;
import com.whyitrose.apiserver.stock.dto.StockDtos.ChangeDirection;
import com.whyitrose.apiserver.stock.dto.StockDtos.CompanyFinancials;
import com.whyitrose.apiserver.stock.dto.StockDtos.InvestorTrading;
import com.whyitrose.apiserver.stock.dto.StockDtos.StockCompanyResponse;
import com.whyitrose.apiserver.stock.dto.StockDtos.StockDetailResponse;
import com.whyitrose.apiserver.stock.dto.StockDtos.StockListItem;
import com.whyitrose.apiserver.stock.dto.StockDtos.StockListResponse;
import com.whyitrose.apiserver.stock.dto.StockDtos.StockPricesResponse;
import com.whyitrose.apiserver.stock.dto.StockDtos.StockSearchItem;
import com.whyitrose.apiserver.stock.dto.StockDtos.StockSearchResponse;
import com.whyitrose.apiserver.stock.dto.StockDtos.TodayOhlcv;
import com.whyitrose.apiserver.stock.exception.StockErrorCode;
import com.whyitrose.apiserver.stock.fss.FssCorpBasicClient;
import com.whyitrose.apiserver.stock.fss.FssCompanyProfile;
import com.whyitrose.apiserver.stock.kis.KisFinancialRatio;
import com.whyitrose.apiserver.stock.kis.KisFinancialRatioClient;
import com.whyitrose.apiserver.stock.kis.KisIncomeStatement;
import com.whyitrose.apiserver.stock.kis.KisInvestorTrading;
import com.whyitrose.apiserver.stock.kis.KisStockBasicInfo;
import com.whyitrose.apiserver.stock.ls.LsCompanyInfo;
import com.whyitrose.apiserver.stock.ls.LsInvestInfoClient;
import com.whyitrose.core.exception.BaseException;
import com.whyitrose.domain.common.Status;
import com.whyitrose.domain.stock.Stock;
import com.whyitrose.domain.stock.StockCompanySnapshot;
import com.whyitrose.domain.stock.StockCompanySnapshotRepository;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class StockService {
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    private final StockRepository stockRepository;
    private final StockPriceRepository stockPriceRepository;
    private final StockListSnapshotRepository stockListSnapshotRepository;
    private final StockCompanySnapshotRepository stockCompanySnapshotRepository;
    private final LsInvestInfoClient lsInvestInfoClient;
    private final KisFinancialRatioClient kisFinancialRatioClient;
    private final FssCorpBasicClient fssCorpBasicClient;
    private final ObjectMapper objectMapper;

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

    public StockCompanyResponse getStockCompany(Long stockId) {
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new BaseException(StockErrorCode.STOCK_001));
        StockCompanySnapshot snapshot = stockCompanySnapshotRepository.findByStockId(stockId)
                .orElseGet(() -> refreshOneStockCompanySnapshot(stock));
        Week52PriceRange week52 = resolveWeek52Range(stock.getId());

        List<String> sectorTags = parseSectorTags(snapshot.getSectorTagsJson());

        return new StockCompanyResponse(
                stock.getId(),
                stock.getTicker(),
                stock.getName(),
                stock.getMarket().name(),
                stock.getLogoUrl(),
                sectorTags,
                nvl(snapshot.getMarketCap()),
                resolveMarketRank(snapshot, stock.getMarket()),
                nvl(snapshot.getTotalShares()),
                nvl(snapshot.getForeignRatio()),
                snapshot.getIndustryGroup(),
                snapshot.getSubIndustry(),
                week52.low(),
                week52.high(),
                snapshot.getOverview(),
                new CompanyFinancials(
                        snapshot.getFinancialBaseDate(),
                        nvl(snapshot.getRevenue()),
                        nvl(snapshot.getRevenueGrowthRate()),
                        nvl(snapshot.getOperatingProfit()),
                        nvl(snapshot.getOperatingProfitGrowthRate()),
                        nvl(snapshot.getNetProfit()),
                        nvl(snapshot.getNetProfitGrowthRate())
                ),
                new InvestorTrading(
                        snapshot.getInvestorBaseDate(),
                        nvl(snapshot.getInvestorForeign()),
                        nvl(snapshot.getInvestorInstitution()),
                        nvl(snapshot.getInvestorIndividual())
                )
        );
    }

    public synchronized void refreshAllStockCompanySnapshots() {
        List<Stock> stocks = stockRepository.findByStatusOrderByIdAsc(Status.ACTIVE);
        int total = stocks.size();
        if (total == 0) {
            log.info("Company snapshot refresh skipped: no active stocks.");
            return;
        }
        int success = 0;
        int failed = 0;
        for (int i = 0; i < total; i++) {
            Stock stock = stocks.get(i);
            int progress = (int) Math.round(((i + 1) * 100.0) / total);
            log.info("Company snapshot progress: {}/{} ({}%) stockId={}, ticker={}",
                    i + 1, total, progress, stock.getId(), stock.getTicker());
            try {
                refreshOneStockCompanySnapshot(stock);
                success++;
            } catch (Exception e) {
                failed++;
                log.error("Company snapshot refresh failed. stockId={}, ticker={}, name={}",
                        stock.getId(), stock.getTicker(), stock.getName(), e);
            }
        }
        refreshMarketRanks();
        log.info("Company snapshot refresh completed. total={}, success={}, failed={}", total, success, failed);
    }

    private StockCompanySnapshot refreshOneStockCompanySnapshot(Stock stock) {
        LsCompanyInfo lsInfo;
        KisIncomeStatement kisIncome;
        KisFinancialRatio kisRatio;
        KisInvestorTrading kisTrading;
        KisStockBasicInfo kisStockBasicInfo;
        try {
            lsInfo = lsInvestInfoClient.fetchCompanyInfo(stock.getTicker());
        } catch (Exception e) {
            throw new IllegalStateException("LS company info failed for ticker=" + stock.getTicker(), e);
        }
        try {
            kisIncome = kisFinancialRatioClient.fetchYearlyIncomeStatement(stock.getTicker());
        } catch (Exception e) {
            throw new IllegalStateException("KIS income statement failed for ticker=" + stock.getTicker(), e);
        }
        try {
            kisRatio = kisFinancialRatioClient.fetchYearlyRatio(stock.getTicker());
        } catch (Exception e) {
            throw new IllegalStateException("KIS financial ratio failed for ticker=" + stock.getTicker(), e);
        }
        try {
            kisTrading = kisFinancialRatioClient.fetchInvestorTradingDaily(stock.getTicker());
        } catch (Exception e) {
            throw new IllegalStateException("KIS investor trading failed for ticker=" + stock.getTicker(), e);
        }
        try {
            kisStockBasicInfo = kisFinancialRatioClient.fetchStockBasicInfo(stock.getTicker());
        } catch (Exception e) {
            throw new IllegalStateException("KIS stock basic info failed for ticker=" + stock.getTicker(), e);
        }
        String industryGroup = normalizeIndustryGroup(lsInfo.industryGroup());
        String subIndustry = kisStockBasicInfo.subIndustry();
        FssCompanyProfile companyProfile = fssCorpBasicClient.fetchCompanyProfile(stock.getName(), industryGroup);
        String overview = companyProfile.overview();
        String sectorTagsJson = toSectorTagsJson(companyProfile.mainBiz());
        String financialBaseDate = kisIncome.baseDate().isBlank() ? kisRatio.baseDate() : kisIncome.baseDate();
        double revenueGrowthRate = resolveRevenueGrowthRate(kisIncome, kisRatio);
        double operatingProfitGrowthRate = resolveProfitRate(kisIncome.operatingProfit(), kisIncome.revenue(), kisRatio.operatingProfitGrowthRate());
        double netProfitGrowthRate = resolveProfitRate(kisIncome.netProfit(), kisIncome.revenue(), kisRatio.netProfitGrowthRate());

        StockCompanySnapshot snapshot = stockCompanySnapshotRepository.findByStockId(stock.getId())
                .orElseGet(() -> StockCompanySnapshot.create(stock));
        snapshot.apply(
                industryGroup,
                subIndustry,
                sectorTagsJson,
                lsInfo.marketCap(),
                lsInfo.totalShares(),
                lsInfo.foreignRatio(),
                overview,
                financialBaseDate,
                kisIncome.revenue(),
                revenueGrowthRate,
                kisIncome.operatingProfit(),
                operatingProfitGrowthRate,
                kisIncome.netProfit(),
                netProfitGrowthRate,
                kisTrading.baseDate(),
                kisTrading.foreignNetBuyAmount(),
                kisTrading.institutionNetBuyAmount(),
                kisTrading.individualNetBuyAmount()
        );
        return stockCompanySnapshotRepository.save(snapshot);
    }

    private Week52PriceRange resolveWeek52Range(Long stockId) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusWeeks(52);
        List<StockPrice> prices = stockPriceRepository.findByStockIdAndPeriodAndTradingDateBetweenOrderByTradingDateAsc(
                stockId, StockPricePeriod.DAILY, from, to);
        if (prices.isEmpty()) {
            return new Week52PriceRange(0L, 0L);
        }
        long low = Long.MAX_VALUE;
        long high = Long.MIN_VALUE;
        for (StockPrice price : prices) {
            low = Math.min(low, price.getLowPrice());
            high = Math.max(high, price.getHighPrice());
        }
        return new Week52PriceRange(low, high);
    }

    private Integer resolveMarketRank(StockCompanySnapshot snapshot, StockMarket market) {
        if (snapshot.getMarketRank() != null) {
            return snapshot.getMarketRank();
        }
        List<StockCompanySnapshot> ranked = rankSnapshotsForMarket(market);
        for (int i = 0; i < ranked.size(); i++) {
            StockCompanySnapshot candidate = ranked.get(i);
            if (candidate.getStock().getId().equals(snapshot.getStock().getId())) {
                return i + 1;
            }
        }
        return null;
    }

    private void refreshMarketRanks() {
        updateMarketRanks(StockMarket.KOSPI);
        updateMarketRanks(StockMarket.KOSDAQ);
    }

    private void updateMarketRanks(StockMarket market) {
        List<StockCompanySnapshot> ranked = rankSnapshotsForMarket(market);
        for (int i = 0; i < ranked.size(); i++) {
            ranked.get(i).updateMarketRank(i + 1);
        }
    }

    private List<StockCompanySnapshot> rankSnapshotsForMarket(StockMarket market) {
        return stockCompanySnapshotRepository
                .findByStockMarketAndStockStatusAndMarketCapIsNotNullOrderByMarketCapDescStockIdAsc(
                        market,
                        Status.ACTIVE
                );
    }

    private PriceSnapshot latestSnapshot(Long stockId, String period) {
        String key = normalizePeriod(period);
        return latestSnapshotForPeriod(stockId, key);
    }

    private PriceSnapshot latestSnapshotForPeriod(Long stockId, String periodKey) {
        return switch (normalizePeriod(periodKey)) {
            case "REALTIME", "1D" -> latestDailySnapshot(stockId);
            case "1W" -> latestFixedPeriodSnapshot(stockId, StockPricePeriod.WEEKLY);
            case "1M" -> latestFixedPeriodSnapshot(stockId, StockPricePeriod.MONTHLY);
            case "1Y" -> latestFixedPeriodSnapshot(stockId, StockPricePeriod.YEARLY);
            case "3M" -> latestRollingDailySnapshot(stockId, 60);
            case "6M" -> latestRollingDailySnapshot(stockId, 120);
            default -> latestRollingDailySnapshot(stockId, 120);
        };
    }

    private PriceSnapshot latestDailySnapshot(Long stockId) {
        List<StockPrice> desc = stockPriceRepository.findTop2ByStockIdAndPeriodOrderByTradingDateDesc(
                stockId, StockPricePeriod.DAILY);
        if (desc.isEmpty()) {
            return emptyPriceSnapshot();
        }
        StockPrice current = desc.get(0);
        StockPrice previous = desc.size() > 1 ? desc.get(1) : current;
        return buildPriceSnapshot(current, previous, current.getVolume(), calcTradingAmount(current));
    }

    private PriceSnapshot latestFixedPeriodSnapshot(Long stockId, StockPricePeriod period) {
        List<StockPrice> desc = stockPriceRepository.findByStockIdAndPeriodOrderByTradingDateDesc(
                stockId, period, PageRequest.of(0, 2));
        if (desc.isEmpty()) {
            return emptyPriceSnapshot();
        }
        StockPrice current = desc.get(0);
        StockPrice previous = desc.size() > 1 ? desc.get(1) : current;
        long tradingVolume = current.getVolume();
        long tradingAmount = calcTradingAmount(current);
        if (tradingVolume == 0L || tradingAmount == 0L) {
            return latestDailySnapshot(stockId);
        }
        return buildPriceSnapshot(current, previous, tradingVolume, tradingAmount);
    }

    private PriceSnapshot latestRollingDailySnapshot(Long stockId, int tradingDays) {
        List<StockPrice> desc = stockPriceRepository.findByStockIdAndPeriodOrderByTradingDateDesc(
                stockId, StockPricePeriod.DAILY, PageRequest.of(0, Math.max(2, tradingDays + 1)));
        if (desc.isEmpty()) {
            return emptyPriceSnapshot();
        }
        StockPrice current = desc.get(0);
        StockPrice previous = desc.size() > tradingDays ? desc.get(tradingDays) : desc.get(desc.size() - 1);
        int windowSize = Math.max(1, Math.min(tradingDays, desc.size()));
        long tradingVolume = 0L;
        long tradingAmount = 0L;
        for (int i = 0; i < windowSize; i++) {
            StockPrice price = desc.get(i);
            tradingVolume += price.getVolume();
            tradingAmount += calcTradingAmount(price);
        }
        if (tradingVolume == 0L || tradingAmount == 0L) {
            tradingVolume = current.getVolume();
            tradingAmount = calcTradingAmount(current);
        }
        return buildPriceSnapshot(current, previous, tradingVolume, tradingAmount);
    }

    private PriceSnapshot buildPriceSnapshot(StockPrice current, StockPrice previous, long tradingVolume, long tradingAmount) {
        long currentPrice = current.getClosePrice();
        long previousPrice = previous == null ? currentPrice : previous.getClosePrice();
        long change = currentPrice - previousPrice;
        double rate = previousPrice == 0 ? 0.0 : ((double) change / previousPrice) * 100.0;
        ChangeDirection direction = change > 0 ? ChangeDirection.UP : (change < 0 ? ChangeDirection.DOWN : ChangeDirection.FLAT);
        return new PriceSnapshot(currentPrice, change, round2(rate), direction, tradingAmount, tradingVolume);
    }

    private long calcTradingAmount(StockPrice price) {
        return (long) price.getClosePrice() * price.getVolume();
    }

    private PriceSnapshot emptyPriceSnapshot() {
        return new PriceSnapshot(0, 0, 0.0, ChangeDirection.FLAT, 0, 0);
    }

    private String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return "TRADING_AMOUNT";
        }
        String key = sort.toUpperCase(Locale.ROOT);
        return switch (key) {
            case "TRADING_VOLUME", "SURGE", "DROP", "TRADING_AMOUNT", "MARKET_CAP" -> key;
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
                case "MARKET_CAP" -> stockListSnapshotRepository.findFirstByMarketCap(periodKey, marketFilter, pageable);
                case "TRADING_VOLUME" -> stockListSnapshotRepository.findFirstByTradingVolume(periodKey, marketFilter, pageable);
                case "SURGE" -> stockListSnapshotRepository.findFirstBySurge(periodKey, marketFilter, pageable);
                case "DROP" -> stockListSnapshotRepository.findFirstByDrop(periodKey, marketFilter, pageable);
                default -> stockListSnapshotRepository.findFirstByTradingAmount(periodKey, marketFilter, pageable);
            };
        }
        return switch (sortKey) {
            case "MARKET_CAP" -> stockListSnapshotRepository.findNextByMarketCap(
                    periodKey,
                    marketFilter,
                    Long.parseLong(cursor.sortValue()),
                    cursor.stockId(),
                    pageable
            );
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
        for (Stock stock : stocks) {
            PriceSnapshot snapshot = latestSnapshotForPeriod(stock.getId(), periodKey);
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
            case "MARKET_CAP" -> new StockListCursor(sortKey, String.valueOf(resolveMarketCapForList(row.getStock().getId())), stockId, nextRank);
            case "TRADING_VOLUME" -> new StockListCursor(sortKey, String.valueOf(row.getTradingVolume()), stockId, nextRank);
            case "SURGE", "DROP" -> new StockListCursor(sortKey, String.valueOf(row.getChangeRate()), stockId, nextRank);
            default -> new StockListCursor(sortKey, String.valueOf(row.getTradingAmount()), stockId, nextRank);
        };
    }

    private long resolveMarketCapForList(Long stockId) {
        return stockCompanySnapshotRepository.findByStockId(stockId)
                .map(StockCompanySnapshot::getMarketCap)
                .orElse(0L);
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

    private double resolveRevenueGrowthRate(KisIncomeStatement income, KisFinancialRatio ratio) {
        double upstream = ratio.revenueGrowthRate();
        if (!isZeroRate(upstream)) {
            return upstream;
        }
        if (income.previousRevenue() == 0L) {
            return upstream;
        }
        return round2(((double) (income.revenue() - income.previousRevenue()) / income.previousRevenue()) * 100.0);
    }

    private double resolveProfitRate(long profit, long revenue, double upstream) {
        if (!isZeroRate(upstream)) {
            return upstream;
        }
        if (revenue == 0L) {
            return upstream;
        }
        return round2(((double) profit / revenue) * 100.0);
    }

    private boolean isZeroRate(double value) {
        return Math.abs(value) < 0.000001d;
    }

    private String normalizeIndustryGroup(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceFirst("^FICS\\s+", "").trim();
    }

    private String toSectorTagsJson(String mainBiz) {
        String normalized = normalizeSectorTag(mainBiz);
        try {
            return normalized.isBlank()
                    ? objectMapper.writeValueAsString(List.of())
                    : objectMapper.writeValueAsString(List.of(normalized));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize sector tags", e);
        }
    }

    private List<String> parseSectorTags(String sectorTagsJson) {
        if (sectorTagsJson == null || sectorTagsJson.isBlank()) {
            return List.of();
        }
        try {
            List<String> values = objectMapper.readValue(sectorTagsJson, STRING_LIST_TYPE);
            return values.stream()
                    .map(this::normalizeSectorTag)
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String normalizeSectorTag(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private long nvl(Long value) {
        return value == null ? 0L : value;
    }

    private double nvl(Double value) {
        return value == null ? 0.0 : value;
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

    private record Week52PriceRange(long low, long high) {}
}
