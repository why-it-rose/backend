package com.whyitrose.domain.event;

import com.whyitrose.domain.common.BaseTimeEntity;
import com.whyitrose.domain.common.Status;
import com.whyitrose.domain.news.News;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(
        name = "event_news",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_event_news", columnNames = {"event_id", "news_id"})
        },
        indexes = {
                // DDL: relevance_score DESC — Hibernate 6.x 지원
                @Index(name = "idx_event_news_score", columnList = "event_id, relevance_score DESC")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventNews extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "news_id", nullable = false)
    private News news;

    // 관련성 점수 0~1
    @Column(name = "relevance_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal relevanceScore;

    // DEFAULT 'ACTIVE'
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status;

    public static EventNews create(Event event, News news, BigDecimal relevanceScore) {
        EventNews eventNews = new EventNews();
        eventNews.event = event;
        eventNews.news = news;
        eventNews.relevanceScore = relevanceScore;
        eventNews.status = Status.ACTIVE;
        return eventNews;
    }

    // 연관관계 편의 메서드
    public void assignEvent(Event event) {
        this.event = event;
        event.getEventNewsList().add(this);
    }

    public void delete() {
        this.status = Status.DELETED;
    }
}