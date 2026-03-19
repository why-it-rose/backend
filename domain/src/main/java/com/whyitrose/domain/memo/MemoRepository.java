package com.whyitrose.domain.memo;

import com.whyitrose.domain.common.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemoRepository extends JpaRepository<Memo, Long> {

    List<Memo> findByUserIdAndEventIdAndStatus(Long userId, Long eventId, Status status);
}