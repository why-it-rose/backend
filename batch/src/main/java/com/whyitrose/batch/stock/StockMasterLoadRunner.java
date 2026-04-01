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
@ConditionalOnProperty(name = "stock.master.load-at-startup", havingValue = "true")
public class StockMasterLoadRunner implements ApplicationRunner {

    private final StockMasterLoadService stockMasterLoadService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("주식 마스터(t9945) 적재 시작");
        stockMasterLoadService.loadAndUpsert();
        log.info("주식 마스터(t9945) 적재 완료");
    }
}
