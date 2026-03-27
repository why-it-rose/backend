package com.whyitrose.domain.stock;

import com.whyitrose.domain.common.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByTicker(String ticker);

    List<Stock> findByStatus(Status status);
    List<Stock> findByStatusOrderByIdAsc(Status status);
    List<Stock> findByStatusAndTickerContainingIgnoreCaseOrderByIdAsc(Status status, String ticker, Pageable pageable);
    List<Stock> findByStatusAndNameContainingIgnoreCaseOrderByIdAsc(Status status, String name, Pageable pageable);

    boolean existsByTicker(String ticker);
}
