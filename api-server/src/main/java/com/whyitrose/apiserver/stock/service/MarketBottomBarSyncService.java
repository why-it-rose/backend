package com.whyitrose.apiserver.stock.service;

import com.whyitrose.apiserver.stock.ls.LsRealtimeQuote;
import com.whyitrose.apiserver.stock.ls.LsRealtimeQuoteClient;
import com.whyitrose.domain.stock.MarketBottomBarItem;
import com.whyitrose.domain.stock.MarketBottomBarItemRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MarketBottomBarSyncService {

    private static final List<BottomBarSeed> SEEDS = List.of(
            new BottomBarSeed("sol-200tr", "SOL 200TR", "295040",
                    "https://www.soletf.co.kr/ko/fund/etf/210734", 1),
            new BottomBarSeed("sol-kosdaq150", "SOL 코스닥150", "450910",
                    "https://www.soletf.co.kr/ko/fund/etf/210961", 2),
            new BottomBarSeed("sol-ai-sobujang", "SOL AI반도체소부장", "455850",
                    "https://www.soletf.co.kr/ko/fund/etf/210980", 3),
            new BottomBarSeed("sol-korea-dividend", "SOL 코리아고배당", "0105E0",
                    "https://www.soletf.co.kr/ko/fund/etf/211097", 4),
            new BottomBarSeed("sol-smr", "SOL 한국원자력SMR", "0092B0",
                    "https://www.soletf.co.kr/ko/fund/etf/211096", 5),
            new BottomBarSeed("sol-ai-top2plus", "SOL AI반도체TOP2플러스", "0167A0",
                    "https://www.soletf.co.kr/ko/fund/etf/211106", 6)
    );

    private final MarketBottomBarItemRepository marketBottomBarItemRepository;
    private final LsRealtimeQuoteClient lsRealtimeQuoteClient;

    public void sync() {
        for (BottomBarSeed seed : SEEDS) {
            MarketBottomBarItem item = marketBottomBarItemRepository.findById(seed.id())
                    .orElseGet(() -> MarketBottomBarItem.create(
                            seed.id(),
                            seed.label(),
                            seed.shcode(),
                            seed.infoUrl(),
                            seed.displayOrder()
                    ));
            item.update(seed.label(), seed.shcode(), seed.infoUrl(), seed.displayOrder());
            LsRealtimeQuote quote = lsRealtimeQuoteClient.fetchQuote(seed.shcode());
            item.updateQuote(quote.currentPrice(), quote.priceChange(), quote.changeRate());
            marketBottomBarItemRepository.save(item);
        }
        log.info("Market bottom bar item sync completed. total={}", SEEDS.size());
    }

    private record BottomBarSeed(
            String id,
            String label,
            String shcode,
            String infoUrl,
            Integer displayOrder
    ) {
    }
}
