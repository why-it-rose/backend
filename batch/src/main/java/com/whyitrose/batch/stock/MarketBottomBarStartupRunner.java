package com.whyitrose.batch.stock;

import com.whyitrose.apiserver.stock.service.MarketBottomBarSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketBottomBarStartupRunner implements ApplicationRunner {

    private final MarketBottomBarSyncService marketBottomBarSyncService;

    @Value("${stock.market-bottom-bar.sync-on-startup:false}")
    private boolean syncOnStartup;

    @Override
    public void run(ApplicationArguments args) {
        if (!syncOnStartup) {
            return;
        }
        log.info("Market bottom bar startup sync started.");
        marketBottomBarSyncService.sync();
        log.info("Market bottom bar startup sync finished.");
    }
}
