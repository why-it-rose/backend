package com.whyitrose.domain.interest;

import com.whyitrose.domain.common.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InterestStockRepository extends JpaRepository<InterestStock, Long> {

    Optional<InterestStock> findByUserIdAndStockId(Long userId, Long stockId);

    @Query("SELECT i FROM InterestStock i JOIN FETCH i.stock "
            + "WHERE i.user.id = :userId AND i.status = :status ORDER BY i.createdAt DESC")
    List<InterestStock> findActiveByUserIdWithStock(
            @Param("userId") Long userId, @Param("status") Status status);

    // 현재 활성 관심종목 목록
    List<InterestStock> findByUserIdAndStatus(Long userId, Status status);

    // idx_interest_stocks_stock 활용 — 종목별 관심 유저 수
    long countByStockIdAndStatus(Long stockId, Status status);
}