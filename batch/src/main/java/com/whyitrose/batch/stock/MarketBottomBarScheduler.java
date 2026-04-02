package com.whyitrose.batch.stock;

import com.whyitrose.apiserver.stock.service.MarketBottomBarSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketBottomBarScheduler {

    private final MarketBottomBarSyncService marketBottomBarSyncService;

    @Scheduled(
            cron = "${stock.market-bottom-bar.sync-cron:0 0 6 * * *}",
            zone = "${stock.market-bottom-bar.sync-zone:Asia/Seoul}"
    )
    public void sync() {
        try {
            marketBottomBarSyncService.sync();
        } catch (Exception e) {
            log.error("Market bottom bar sync failed", e);
        }
    }
}
