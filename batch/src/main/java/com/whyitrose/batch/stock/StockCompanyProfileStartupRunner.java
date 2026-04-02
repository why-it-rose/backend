package com.whyitrose.batch.stock;

import com.whyitrose.apiserver.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockCompanyProfileStartupRunner implements CommandLineRunner {

    private final StockService stockService;

    @Value("${stock.company.refresh-on-startup:false}")
    private boolean refreshOnStartup;

    @Override
    public void run(String... args) {
        if (!refreshOnStartup) {
            return;
        }
        log.info("Stock company profile startup refresh started.");
        stockService.refreshAllStockCompanySnapshots();
        log.info("Stock company profile startup refresh finished.");
    }
}
