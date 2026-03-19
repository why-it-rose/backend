package com.whyitrose.domain.stock;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockPriceRepository extends JpaRepository<StockPrice, Long> {

    Optional<StockPrice> findByStockIdAndTradingDate(Long stockId, LocalDate tradingDate);

    List<StockPrice> findByStockIdAndTradingDateBetweenOrderByTradingDateAsc(
            Long stockId, LocalDate from, LocalDate to);

    // 특정 종목의 전체 주가를 날짜 오름차순 조회
    List<StockPrice> findByStockIdOrderByTradingDateAsc(Long stockId);

    // 특정 날짜 이전 최근 N개 거래일 주가 조회 (거래량 평균 계산용)
    @Query("SELECT sp FROM StockPrice sp " +
           "WHERE sp.stock.id = :stockId AND sp.tradingDate < :targetDate " +
           "ORDER BY sp.tradingDate DESC")
    List<StockPrice> findRecentPricesBeforeDate(
            @Param("stockId") Long stockId,
            @Param("targetDate") LocalDate targetDate,
            Pageable pageable);
}