package com.whyitrose.apiserver.stock.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockSnapshotScheduler {

    private final StockService stockService;

    @Scheduled(
            cron = "${stock.snapshot.refresh-cron:0 0 5 * * *}",
            zone = "${stock.snapshot.refresh-zone:Asia/Seoul}"
    )
    public void refreshSnapshots() {
        stockService.refreshAllStockListSnapshots();
    }
}
