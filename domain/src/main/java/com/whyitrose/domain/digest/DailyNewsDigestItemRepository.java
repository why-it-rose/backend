package com.whyitrose.domain.digest;

import com.whyitrose.domain.common.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DailyNewsDigestItemRepository extends JpaRepository<DailyNewsDigestItem, Long> {

    // idx_digest_items_stock 활용 — 알림센터 종목 필터링
    List<DailyNewsDigestItem> findByDigestIdAndStockIdAndStatus(Long digestId, Long stockId, Status status);

    List<DailyNewsDigestItem> findByDigestIdAndStatus(Long digestId, Status status);
}