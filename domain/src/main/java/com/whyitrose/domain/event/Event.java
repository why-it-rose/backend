package com.whyitrose.domain.event;

import com.whyitrose.domain.common.BaseTimeEntity;
import com.whyitrose.domain.common.Status;
import com.whyitrose.domain.stock.Stock;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 급등/급락 이벤트 엔티티
 *
 * <p>특정 종목에서 가격 변동(±5% 이상)과 거래량 급증(20일 평균의 1.5배 이상)이
 * 동시에 발생한 날을 이벤트로 기록한다.
 *
 * <h3>이벤트 생명주기</h3>
 * <pre>
 * 배치 탐지 → PENDING (status, crawlStatus 모두 PENDING)
 *     ↓ AI 요약 + 뉴스 크롤링 완료
 * ACTIVE (사용자 노출)
 * </pre>
 *
 * <h3>이벤트 병합</h3>
 * 연속된 거래일에 같은 방향(SURGE/DROP)의 이벤트가 탐지되면 하나의 이벤트로 병합한다.
 * 최대 3거래일까지 병합 가능하며, 누적 변동률은 startDate 전일 종가 기준으로 재계산된다.
 */
@Entity
@Table(
        name = "events",
        indexes = {
                @Index(name = "idx_events_stock_date", columnList = "stock_id, start_date"),
                @Index(name = "idx_events_status", columnList = "status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    /** 이벤트 방향: SURGE(급등) | DROP(급락) */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 10, nullable = false)
    private EventType eventType;

    /** 이벤트 시작일 — 병합 이벤트의 경우 최초 탐지일 */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** 이벤트 종료일 — 단일 이벤트면 startDate와 동일, 병합 시 마지막 탐지일로 갱신 */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** startDate 기준 전일 종가 → endDate 종가 기준 누적 등락률 (%) */
    @Column(name = "change_pct", nullable = false, precision = 6, scale = 2)
    private BigDecimal changePct;

    /** startDate 전일 종가 스냅샷 — 병합 시 기준점으로 사용되므로 변경하지 않는다 */
    @Column(name = "price_before", nullable = false)
    private int priceBefore;

    /** endDate 종가 스냅샷 — 병합 시 최신 종가로 갱신된다 */
    @Column(name = "price_after", nullable = false)
    private int priceAfter;

    /**
     * 이벤트에 포함된 거래일 수 (단일=1, 최대=3)
     *
     * <p>병합 기준을 3일로 제한하는 이유: 장기 추세가 아닌 단기 급변동 이슈를
     * 탐지하는 것이 목적이므로, 너무 긴 기간의 병합은 의미를 희석시킨다.
     */
    @Column(name = "trading_days_count", nullable = false)
    private int tradingDaysCount;

    /**
     * AI가 생성한 이벤트 요약 (1~3문장)
     *
     * <p>배치 탐지 시점에는 null이며, AI 요약 생성 후 {@link #activate(String)}를 통해 채워진다.
     */
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    /**
     * 이벤트 공개 상태
     * <ul>
     *   <li>PENDING: AI 요약 생성 대기 중 — 사용자에게 노출되지 않음</li>
     *   <li>ACTIVE: 요약 완료 — 사용자에게 노출</li>
     *   <li>DELETED: 삭제됨</li>
     * </ul>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status;

    /**
     * 관련 뉴스 크롤링 상태
     * <ul>
     *   <li>PENDING: 크롤링 대기 중</li>
     *   <li>ACTIVE: 크롤링 완료</li>
     *   <li>INACTIVE: 크롤링 실패</li>
     * </ul>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "crawl_status", length = 20, nullable = false)
    private Status crawlStatus;

    @OneToMany(mappedBy = "event", fetch = FetchType.LAZY)
    private List<EventNews> eventNewsList = new ArrayList<>();

    /**
     * 이벤트 팩토리 메서드
     *
     * <p>status와 crawlStatus를 모두 PENDING으로 초기화한다.
     * AI 요약 생성 + 뉴스 크롤링이 완료된 후 ACTIVE로 전환된다.
     */
    public static Event create(Stock stock, EventType eventType,
                               LocalDate startDate, LocalDate endDate,
                               BigDecimal changePct, int priceBefore, int priceAfter) {
        Event event = new Event();
        event.stock = stock;
        event.eventType = eventType;
        event.startDate = startDate;
        event.endDate = endDate;
        event.changePct = changePct;
        event.priceBefore = priceBefore;
        event.priceAfter = priceAfter;
        event.tradingDaysCount = 1;
        event.status = Status.PENDING;
        event.crawlStatus = Status.PENDING;
        return event;
    }

    public void assignStock(Stock stock) {
        this.stock = stock;
        stock.getEvents().add(this);
    }

    /**
     * AI 요약이 완성되면 이벤트를 ACTIVE 상태로 전환한다.
     *
     * @param summary AI가 생성한 요약 (1~3문장)
     */
    public void activate(String summary) {
        this.summary = summary;
        this.status = Status.ACTIVE;
    }

    /**
     * 연속 이벤트 병합 — 기간을 다음 거래일로 확장한다.
     *
     * <p>priceBefore는 변경하지 않는다. 누적 변동률 계산의 기준점이기 때문이다.
     * changePct는 호출자({@link EventSaveService})가 priceBefore 기준으로 재계산한 값을 전달한다.
     *
     * @param newEndDate    새 종료일
     * @param newPriceAfter 새 종가
     * @param newChangePct  재계산된 누적 변동률
     */
    public void extend(LocalDate newEndDate, int newPriceAfter, BigDecimal newChangePct) {
        this.endDate = newEndDate;
        this.priceAfter = newPriceAfter;
        this.changePct = newChangePct;
        this.tradingDaysCount++;
    }

    public void delete() {
        this.status = Status.DELETED;
    }
}
