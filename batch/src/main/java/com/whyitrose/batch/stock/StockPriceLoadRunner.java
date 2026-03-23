package com.whyitrose.batch.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stock.price.load-at-startup", havingValue = "true")
public class StockPriceLoadRunner implements ApplicationRunner {

    private final StockPriceLoadService stockPriceLoadService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("주가 차트(t8451) 적재 시작");
        stockPriceLoadService.loadAllPeriodsForActiveStocks();
        log.info("주가 차트(t8451) 적재 완료");
    }
}
