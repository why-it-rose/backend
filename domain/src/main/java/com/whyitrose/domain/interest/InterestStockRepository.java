package com.whyitrose.domain.interest;

import com.whyitrose.domain.common.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterestStockRepository extends JpaRepository<InterestStock, Long> {

    Optional<InterestStock> findByUserIdAndStockId(Long userId, Long stockId);

    // 현재 활성 관심종목 목록
    List<InterestStock> findByUserIdAndStatus(Long userId, Status status);

    // idx_interest_stocks_stock 활용 — 종목별 관심 유저 수
    long countByStockIdAndStatus(Long stockId, Status status);
}