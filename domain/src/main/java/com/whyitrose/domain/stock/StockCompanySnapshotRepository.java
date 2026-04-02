package com.whyitrose.domain.stock;

import com.whyitrose.domain.common.Status;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockCompanySnapshotRepository extends JpaRepository<StockCompanySnapshot, Long> {

    Optional<StockCompanySnapshot> findByStockId(Long stockId);

    List<StockCompanySnapshot> findByStockIdIn(Collection<Long> stockIds);

    List<StockCompanySnapshot> findByStockMarketAndStockStatusAndMarketCapIsNotNullOrderByMarketCapDescStockIdAsc(
            StockMarket market,
            Status status
    );
}
