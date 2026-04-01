package com.whyitrose.domain.memo;

import com.whyitrose.domain.common.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemoRepository extends JpaRepository<Memo, Long> {

    List<Memo> findByUserIdAndEventIdAndStatusOrderByCreatedAtDesc(Long userId, Long eventId, Status status);

    Optional<Memo> findByIdAndUserIdAndStatus(Long id, Long userId, Status status);
}