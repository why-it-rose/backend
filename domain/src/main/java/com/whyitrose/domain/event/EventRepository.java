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
}