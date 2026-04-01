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

    /**
     * 종목별 이벤트 목록 조회 (EventType 필터 없음)
     *
     * <p>startDate 내림차순 정렬. 페이지네이션 적용.
     */
    List<Event> findByStockIdAndStatusOrderByStartDateDesc(Long stockId, Status status, Pageable pageable);

    /**
     * 종목별 이벤트 목록 조회 (EventType 필터 있음)
     *
     * <p>SURGE 또는 DROP 중 하나로 필터링할 때 사용. startDate 내림차순 정렬.
     */
    List<Event> findByStockIdAndStatusAndEventTypeOrderByStartDateDesc(Long stockId, Status status, EventType eventType, Pageable pageable);

    List<Event> findByStatus(Status status);

    /**
     * 이벤트 상세 조회 — 연관 뉴스 JOIN FETCH
     *
     * <p>EventNews와 News를 한 번의 쿼리로 함께 로딩해 N+1 문제를 방지한다.
     * 뉴스는 연관도(relevanceScore) 높은 순으로 정렬된다.
     */
    @Query("""
           SELECT e FROM Event e
           LEFT JOIN FETCH e.eventNewsList en
           LEFT JOIN FETCH en.news
           WHERE e.id = :id
           ORDER BY en.relevanceScore DESC
           """)
    Optional<Event> findByIdWithNews(@Param("id") Long id);

    /**
     * 동일 stock_id + start_date 이벤트 존재 여부 확인
     *
     * <p>재실행 등으로 인한 중복 저장을 방지하기 위해 사용한다.
     * eventType은 체크하지 않는다 — 같은 날 같은 종목에 SURGE·DROP이 동시에 발생하는 것은
     * 가격 조건상 불가능하기 때문이다.
     */
    boolean existsByStockIdAndStartDate(Long stockId, LocalDate startDate);

    /**
     * 병합 대상 이벤트 조회
     *
     * <p>아래 조건을 모두 만족하는 이벤트를 조회한다.
     * <ul>
     *   <li>동일 종목 (stockId)</li>
     *   <li>동일 방향 (eventType: SURGE 또는 DROP)</li>
     *   <li>PENDING 상태 — ACTIVE 전환된 이벤트는 이미 요약이 완성됐으므로 병합하지 않는다</li>
     *   <li>endDate = prevTradingDate — 연속 거래일 연장만 허용</li>
     *   <li>tradingDaysCount &lt; 3 — 최대 3거래일까지만 병합</li>
     * </ul>
     */
    @Query("SELECT e FROM Event e " +
           "WHERE e.stock.id = :stockId " +
           "AND e.eventType = :eventType " +
           "AND e.status = com.whyitrose.domain.common.Status.PENDING " +
           "AND e.endDate = :prevTradingDate " +
           "AND e.tradingDaysCount < 3")
    Optional<Event> findMergeable(@Param("stockId") Long stockId,
                                  @Param("eventType") EventType eventType,
                                  @Param("prevTradingDate") LocalDate prevTradingDate);

    /**
     * 여러 종목의 최신 이벤트 각 1건씩 조회
     *
     * <p>종목 목록 화면에서 각 종목의 가장 최근 이벤트를 한 번에 가져올 때 사용한다.
     * 종목별로 endDate가 가장 큰 이벤트 1건을 반환한다.
     */
    @Query("SELECT e FROM Event e " +
           "WHERE e.stock.id IN :stockIds " +
           "AND e.status = :status " +
           "AND e.endDate = (SELECT MAX(e2.endDate) FROM Event e2 WHERE e2.stock.id = e.stock.id AND e2.status = :status)")
    List<Event> findLatestByStockIds(@Param("stockIds") List<Long> stockIds,
                                     @Param("status") Status status);
}
