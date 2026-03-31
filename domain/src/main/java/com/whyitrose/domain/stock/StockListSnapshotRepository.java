package com.whyitrose.domain.stock;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockListSnapshotRepository extends JpaRepository<StockListSnapshot, Long> {

    List<StockListSnapshot> findByPeriodKeyAndStockIdIn(String periodKey, Collection<Long> stockIds);

    @Query("""
            SELECT s FROM StockListSnapshot s
            JOIN FETCH s.stock st
            WHERE s.periodKey = :periodKey
              AND (:market IS NULL OR st.market = :market)
            ORDER BY s.tradingAmount DESC, st.id ASC
            """)
    List<StockListSnapshot> findFirstByTradingAmount(
            @Param("periodKey") String periodKey,
            @Param("market") StockMarket market,
            Pageable pageable
    );

    @Query("""
            SELECT s FROM StockListSnapshot s
            JOIN FETCH s.stock st
            WHERE s.periodKey = :periodKey
              AND (:market IS NULL OR st.market = :market)
              AND (s.tradingAmount < :cursorValue
                   OR (s.tradingAmount = :cursorValue AND st.id > :cursorStockId))
            ORDER BY s.tradingAmount DESC, st.id ASC
            """)
    List<StockListSnapshot> findNextByTradingAmount(
            @Param("periodKey") String periodKey,
            @Param("market") StockMarket market,
            @Param("cursorValue") long cursorValue,
            @Param("cursorStockId") long cursorStockId,
            Pageable pageable
    );

    @Query("""
            SELECT s FROM StockListSnapshot s
            JOIN FETCH s.stock st
            WHERE s.periodKey = :periodKey
              AND (:market IS NULL OR st.market = :market)
            ORDER BY s.tradingVolume DESC, st.id ASC
            """)
    List<StockListSnapshot> findFirstByTradingVolume(
            @Param("periodKey") String periodKey,
            @Param("market") StockMarket market,
            Pageable pageable
    );

    @Query("""
            SELECT s FROM StockListSnapshot s
            JOIN FETCH s.stock st
            WHERE s.periodKey = :periodKey
              AND (:market IS NULL OR st.market = :market)
              AND (s.tradingVolume < :cursorValue
                   OR (s.tradingVolume = :cursorValue AND st.id > :cursorStockId))
            ORDER BY s.tradingVolume DESC, st.id ASC
            """)
    List<StockListSnapshot> findNextByTradingVolume(
            @Param("periodKey") String periodKey,
            @Param("market") StockMarket market,
            @Param("cursorValue") long cursorValue,
            @Param("cursorStockId") long cursorStockId,
            Pageable pageable
    );

    @Query("""
            SELECT s FROM StockListSnapshot s
            JOIN FETCH s.stock st
            WHERE s.periodKey = :periodKey
              AND (:market IS NULL OR st.market = :market)
            ORDER BY s.changeRate DESC, st.id ASC
            """)
    List<StockListSnapshot> findFirstBySurge(
            @Param("periodKey") String periodKey,
            @Param("market") StockMarket market,
            Pageable pageable
    );

    @Query("""
            SELECT s FROM StockListSnapshot s
            JOIN FETCH s.stock st
            WHERE s.periodKey = :periodKey
              AND (:market IS NULL OR st.market = :market)
              AND (s.changeRate < :cursorValue
                   OR (s.changeRate = :cursorValue AND st.id > :cursorStockId))
            ORDER BY s.changeRate DESC, st.id ASC
            """)
    List<StockListSnapshot> findNextBySurge(
            @Param("periodKey") String periodKey,
            @Param("market") StockMarket market,
            @Param("cursorValue") double cursorValue,
            @Param("cursorStockId") long cursorStockId,
            Pageable pageable
    );

    @Query("""
            SELECT s FROM StockListSnapshot s
            JOIN FETCH s.stock st
            WHERE s.periodKey = :periodKey
              AND (:market IS NULL OR st.market = :market)
            ORDER BY s.changeRate ASC, st.id ASC
            """)
    List<StockListSnapshot> findFirstByDrop(
            @Param("periodKey") String periodKey,
            @Param("market") StockMarket market,
            Pageable pageable
    );

    @Query("""
            SELECT s FROM StockListSnapshot s
            JOIN FETCH s.stock st
            WHERE s.periodKey = :periodKey
              AND (:market IS NULL OR st.market = :market)
              AND (s.changeRate > :cursorValue
                   OR (s.changeRate = :cursorValue AND st.id > :cursorStockId))
            ORDER BY s.changeRate ASC, st.id ASC
            """)
    List<StockListSnapshot> findNextByDrop(
            @Param("periodKey") String periodKey,
            @Param("market") StockMarket market,
            @Param("cursorValue") double cursorValue,
            @Param("cursorStockId") long cursorStockId,
            Pageable pageable
    );
}
