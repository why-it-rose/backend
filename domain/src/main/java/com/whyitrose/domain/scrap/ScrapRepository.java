package com.whyitrose.domain.scrap;

import com.whyitrose.domain.common.Status;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScrapRepository extends JpaRepository<Scrap, Long> {

    Optional<Scrap> findByUserIdAndEventId(Long userId, Long eventId);

    long countByUserIdAndStatus(Long userId, Status status);

    List<Scrap> findByUserIdAndStatus(Long userId, Status status, Pageable pageable);
}