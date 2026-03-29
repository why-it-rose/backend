package com.whyitrose.domain.event;

import com.whyitrose.domain.common.Status;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    // 종목별 이벤트 목록 (타입 필터 없음)
    List<Event> findByStockIdAndStatusOrderByStartDateDesc(Long stockId, Status status, Pageable pageable);

    // 종목별 이벤트 목록 (타입 필터 있음)
    List<Event> findByStockIdAndStatusAndEventTypeOrderByStartDateDesc(Long stockId, Status status, EventType eventType, Pageable pageable);

    List<Event> findByStatus(Status status);

    // 이벤트 상세 + 연관 뉴스 JOIN FETCH (N+1 방지)
    @Query("SELECT e FROM Event e " +
           "LEFT JOIN FETCH e.eventNewsList en " +
           "LEFT JOIN FETCH en.news " +
           "WHERE e.id = :id")
    Optional<Event> findByIdWithNews(@Param("id") Long id);

    // 동일 stock_id + start_date 중복 여부 확인
    boolean existsByStockIdAndStartDate(Long stockId, LocalDate startDate);

    // 병합 대상 이벤트 조회
    // 같은 종목 + 같은 EventType + PENDING + endDate = 직전 거래일 + 거래일 수 3일 미만
    @Query("SELECT e FROM Event e " +
           "WHERE e.stock.id = :stockId " +
           "AND e.eventType = :eventType " +
           "AND e.status = com.whyitrose.domain.common.Status.PENDING " +
           "AND e.endDate = :prevTradingDate " +
           "AND e.tradingDaysCount < 3")
    Optional<Event> findMergeable(@Param("stockId") Long stockId,
                                  @Param("eventType") EventType eventType,
                                  @Param("prevTradingDate") LocalDate prevTradingDate);

    // 여러 종목의 최신 이벤트 조회 (종목 목록 화면용)
    @Query("SELECT e FROM Event e " +
           "WHERE e.stock.id IN :stockIds " +
           "AND e.status = :status " +
           "AND e.endDate = (SELECT MAX(e2.endDate) FROM Event e2 WHERE e2.stock.id = e.stock.id AND e2.status = :status)")
    List<Event> findLatestByStockIds(@Param("stockIds") List<Long> stockIds,
                                     @Param("status") Status status);
}