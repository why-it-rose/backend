package com.whyitrose.domain.scrap;

import com.whyitrose.domain.common.Status;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScrapRepository extends JpaRepository<Scrap, Long> {

    Optional<Scrap> findByUserIdAndEventId(Long userId, Long eventId);

    // idx_scraps_user 활용 — created_at DESC 정렬
    List<Scrap> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Status status, Pageable pageable);
}