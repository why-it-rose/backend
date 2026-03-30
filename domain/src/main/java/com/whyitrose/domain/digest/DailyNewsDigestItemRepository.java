package com.whyitrose.domain.digest;

import com.whyitrose.domain.common.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DailyNewsDigestItemRepository extends JpaRepository<DailyNewsDigestItem, Long> {

    // idx_digest_items_stock 활용 — 알림센터 종목 필터링
    List<DailyNewsDigestItem> findByDigestIdAndStockIdAndStatus(Long digestId, Long stockId, Status status);

    List<DailyNewsDigestItem> findByDigestIdAndStatus(Long digestId, Status status);

    // FCM 개인화 메시지용 — stock명 + 뉴스 수 계산을 위해 stock/news 함께 조회
    @Query("SELECT i FROM DailyNewsDigestItem i JOIN FETCH i.stock JOIN FETCH i.news WHERE i.digest.id = :digestId AND i.status = 'ACTIVE'")
    List<DailyNewsDigestItem> findByDigestIdWithStockAndNews(@Param("digestId") Long digestId);

    // 알림 목록 조회용 — N+1 방지: digestId IN 절로 한번에 조회
    @Query("SELECT i FROM DailyNewsDigestItem i JOIN FETCH i.stock JOIN FETCH i.news WHERE i.digest.id IN :digestIds AND i.status = 'ACTIVE'")
    List<DailyNewsDigestItem> findByDigestIdInWithStockAndNews(@Param("digestIds") List<Long> digestIds);
}