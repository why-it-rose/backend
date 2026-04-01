package com.whyitrose.domain.event;

import com.whyitrose.domain.common.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventNewsRepository extends JpaRepository<EventNews, Long> {

    Optional<EventNews> findByEventIdAndNewsId(Long eventId, Long newsId);

    // 관련성 점수 내림차순 — idx_event_news_score 활용
    List<EventNews> findByEventIdAndStatusOrderByRelevanceScoreDesc(Long eventId, Status status);
}