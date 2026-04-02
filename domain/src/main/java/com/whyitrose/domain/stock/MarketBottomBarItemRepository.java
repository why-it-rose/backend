package com.whyitrose.domain.stock;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketBottomBarItemRepository extends JpaRepository<MarketBottomBarItem, String> {

    List<MarketBottomBarItem> findAllByOrderByDisplayOrderAsc();
}
