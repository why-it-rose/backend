package com.whyitrose.domain.event;

import com.whyitrose.domain.common.Status;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByStockIdAndStatus(Long stockId, Status status, Pageable pageable);

    List<Event> findByStatus(Status status);
}