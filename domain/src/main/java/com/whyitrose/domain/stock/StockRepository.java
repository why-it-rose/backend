package com.whyitrose.domain.stock;

import com.whyitrose.domain.common.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByTicker(String ticker);

    List<Stock> findByStatus(Status status);

    boolean existsByTicker(String ticker);
}