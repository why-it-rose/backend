package com.whyitrose.batch.stock;

import com.whyitrose.apiserver.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockCompanyProfileScheduler {

    private final StockService stockService;

    @Scheduled(
            cron = "${stock.company.snapshot-cron:0 0 4 * * *}",
            zone = "${stock.company.snapshot-zone:Asia/Seoul}"
    )
    public void refreshCompanyProfiles() {
        try {
            stockService.refreshAllStockCompanySnapshots();
        } catch (Exception e) {
            log.error("Stock company profile snapshot refresh failed", e);
        }
    }
}
