package com.whyitrose.domain.scrap;

import com.whyitrose.domain.common.Status;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface ScrapRepository extends JpaRepository<Scrap, Long> {

    Optional<Scrap> findByUserIdAndEventId(Long userId, Long eventId);

    long countByUserIdAndStatus(Long userId, Status status);

    Page<Scrap> findByUserIdAndStatus(Long userId, Status status, Pageable pageable);

    Page<Scrap> findByUserIdAndStatusAndEvent_Stock_NameContainingIgnoreCaseOrUserIdAndStatusAndEvent_Stock_TickerContainingIgnoreCase(
            Long userId1, Status status1, String stockNameKeyword,
            Long userId2, Status status2, String tickerKeyword,
            Pageable pageable
    );

}

