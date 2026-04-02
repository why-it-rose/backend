package com.whyitrose.apiserver.stock.dto;

import java.time.LocalDate;
import java.util.List;

public class StockDtos {

    public enum ChangeDirection {
        UP, DOWN, FLAT
    }

    public record StockListResponse(
            String nextCursor,
            boolean hasNext,
            int size,
            List<StockListItem> items
    ) {}

    public record StockListItem(
            int rank,
            Long stockId,
            String ticker,
            String name,
            String market,
            String logoUrl,
            long currentPrice,
            long priceChange,
            double changeRate,
            ChangeDirection changeDirection,
            long tradingAmount,
            long tradingVolume,
            boolean hasEvent,
            String eventType,
            boolean isInterested
    ) {}

    public record StockSearchResponse(
            String query,
            int totalCount,
            List<StockSearchItem> items
    ) {}

    public record InterestStockListResponse(
            List<StockSearchItem> items
    ) {}

    public record StockSearchItem(
            Long stockId,
            String ticker,
            String name,
            String market,
            String logoUrl,
            long currentPrice,
            double changeRate,
            ChangeDirection changeDirection
    ) {}

    public record MarketBottomBarResponse(
            List<MarketBottomBarItemDto> items
    ) {}

    public record MarketBottomBarItemDto(
            String id,
            String label,
            String shcode,
            String infoUrl,
            long currentPrice,
            long priceChange,
            double changeRate,
            ChangeDirection changeDirection
    ) {}

    public record StockDetailResponse(
            Long stockId,
            String ticker,
            String name,
            String market,
            String sector,
            String logoUrl,
            long currentPrice,
            long priceChange,
            double changeRate,
            ChangeDirection changeDirection,
            TodayOhlcv todayOhlcv,
            boolean isInterested
    ) {}

    public record StockCompanyResponse(
            Long stockId,
            String ticker,
            String name,
            String market,
            String logoUrl,
            List<String> sectorTags,
            long marketCap,
            Integer marketRank,
            long totalShares,
            double foreignRatio,
            String industryGroup,
            String subIndustry,
            long week52Low,
            long week52High,
            String overview,
            CompanyFinancials financials,
            InvestorTrading investorTrading
    ) {}

    public record CompanyFinancials(
            String baseDate,
            long revenue,
            double revenueGrowthRate,
            long operatingProfit,
            double operatingProfitGrowthRate,
            long netProfit,
            double netProfitGrowthRate
    ) {}

    public record InvestorTrading(
            String baseDate,
            long foreign,
            long institution,
            long individual
    ) {}

    public record TodayOhlcv(
            long open,
            long high,
            long low,
            long volume
    ) {}

    public record StockPricesResponse(
            Long stockId,
            String period,
            List<CandleDto> candles,
            List<EventPinDto> eventPins,
            List<NewsPinDto> newsPins
    ) {}

    public record CandleDto(
            LocalDate date,
            long open,
            long close,
            long high,
            long low,
            long volume
    ) {}

    public record EventPinDto(
            Long eventId,
            LocalDate date,
            String eventType,
            double changeRate
    ) {}

    public record NewsPinDto(
            LocalDate date,
            int newsCount
    ) {}
}
