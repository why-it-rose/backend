package com.whyitrose.batch.stock;

import com.whyitrose.apiserver.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockListSnapshotStartupRunner implements CommandLineRunner {

    private final StockService stockService;
    private final ConfigurableApplicationContext applicationContext;

    @Value("${stock.snapshot.refresh-on-startup:false}")
    private boolean refreshOnStartup;

    @Value("${stock.snapshot.exit-after-startup-refresh:false}")
    private boolean exitAfterRefresh;

    @Override
    public void run(String... args) {
        if (!refreshOnStartup) {
            return;
        }
        log.info("Stock list snapshot startup refresh started.");
        stockService.refreshAllStockListSnapshots();
        log.info("Stock list snapshot startup refresh finished.");

        if (exitAfterRefresh) {
            log.info("Stock list snapshot startup refresh exit requested.");
            SpringApplication.exit(applicationContext, () -> 0);
        }
    }
}
