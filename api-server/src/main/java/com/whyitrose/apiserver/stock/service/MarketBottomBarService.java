package com.whyitrose.apiserver.stock.service;

import com.whyitrose.apiserver.stock.dto.StockDtos.ChangeDirection;
import com.whyitrose.apiserver.stock.dto.StockDtos.MarketBottomBarItemDto;
import com.whyitrose.apiserver.stock.dto.StockDtos.MarketBottomBarResponse;
import com.whyitrose.domain.stock.MarketBottomBarItem;
import com.whyitrose.domain.stock.MarketBottomBarItemRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketBottomBarService {

    private final MarketBottomBarItemRepository marketBottomBarItemRepository;

    public MarketBottomBarResponse getItems() {
        List<MarketBottomBarItemDto> items = marketBottomBarItemRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(this::toItem)
                .toList();
        return new MarketBottomBarResponse(items);
    }

    private MarketBottomBarItemDto toItem(MarketBottomBarItem item) {
        long priceChange = item.getPriceChange();
        ChangeDirection direction = priceChange > 0
                ? ChangeDirection.UP
                : (priceChange < 0 ? ChangeDirection.DOWN : ChangeDirection.FLAT);
        return new MarketBottomBarItemDto(
                item.getId(),
                item.getLabel(),
                item.getShcode(),
                item.getInfoUrl(),
                item.getCurrentPrice(),
                priceChange,
                item.getChangeRate(),
                direction
        );
    }
}
