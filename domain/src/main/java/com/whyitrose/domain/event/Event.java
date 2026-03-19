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

    // SURGE | DROP
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 10, nullable = false)
    private EventType eventType;

    // 이벤트 시작일
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    // 이벤트 종료일 — 단일이면 start_date와 동일
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    // 기간 누적 등락률
    @Column(name = "change_pct", nullable = false, precision = 6, scale = 2)
    private BigDecimal changePct;

    // 시작일 전일 종가 스냅샷
    @Column(name = "price_before", nullable = false)
    private int priceBefore;

    // 종료일 종가 스냅샷
    @Column(name = "price_after", nullable = false)
    private int priceAfter;

    // AI 요약 1~3문장
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    // DEFAULT 'PENDING'
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status;

    @OneToMany(mappedBy = "event", fetch = FetchType.LAZY)
    private List<EventNews> eventNewsList = new ArrayList<>();

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
        event.status = Status.PENDING;
        return event;
    }

    // 연관관계 편의 메서드
    public void assignStock(Stock stock) {
        this.stock = stock;
        stock.getEvents().add(this);
    }

    public void activate(String summary) {
        this.summary = summary;
        this.status = Status.ACTIVE;
    }

    public void delete() {
        this.status = Status.DELETED;
    }
}