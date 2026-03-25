package com.whyitrose.batch.stock;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stock.price.scheduler.enabled", havingValue = "true")
public class StockPriceDailyScheduler {

    private final JobLauncher jobLauncher;
    @Qualifier("stockPriceLoadJob")
    private final Job stockPriceLoadJob;

    @Scheduled(cron = "${stock.price.daily-cron:0 03 09 * * MON-FRI}", zone = "Asia/Seoul")
    public void runDailyUpdate() {
        log.info("데일리 주가 업데이트 스케줄 시작: runDate={}", LocalDate.now());

        JobParameters params = new JobParametersBuilder()
                .addLocalDate("runDate", LocalDate.now())
                .toJobParameters();

        try {
            JobExecution execution = jobLauncher.run(stockPriceLoadJob, params);
            log.info("데일리 주가 업데이트 완료: status={}, runDate={}", execution.getStatus(), LocalDate.now());
        } catch (JobInstanceAlreadyCompleteException e) {
            log.info("데일리 주가 업데이트 이미 완료됨 (오늘 중복 실행): runDate={}", LocalDate.now());
        } catch (Exception e) {
            log.error("데일리 주가 업데이트 실패: runDate={}", LocalDate.now(), e);
        }
    }
}