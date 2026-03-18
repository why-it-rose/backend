package com.whyitrose.domain.news;

import com.whyitrose.domain.common.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NewsStockRepository extends JpaRepository<NewsStock, Long> {

    Optional<NewsStock> findByNewsIdAndStockId(Long newsId, Long stockId);

    List<NewsStock> findByStockIdAndStatus(Long stockId, Status status);
}