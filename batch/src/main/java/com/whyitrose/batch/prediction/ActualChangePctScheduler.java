package com.whyitrose.batch.prediction;

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

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "prediction.scheduler.enabled", havingValue = "true")
public class ActualChangePctScheduler {

    private final JobLauncher jobLauncher;
    @Qualifier("actualChangePctJob")
    private final Job actualChangePctJob;

    @Scheduled(cron = "${prediction.scheduler.cron:0 0 18 * * *}", zone = "Asia/Seoul")
    public void run() {
        log.info("[ActualChangePctScheduler] 배치 시작: runDate={}", LocalDate.now());

        JobParameters params = new JobParametersBuilder()
                .addLocalDate("runDate", LocalDate.now())
                .toJobParameters();

        try {
            JobExecution execution = jobLauncher.run(actualChangePctJob, params);
            log.info("[ActualChangePctScheduler] 배치 완료: status={}", execution.getStatus());
        } catch (JobInstanceAlreadyCompleteException e) {
            log.info("[ActualChangePctScheduler] 이미 완료됨 (오늘 중복 실행): runDate={}", LocalDate.now());
        } catch (Exception e) {
            log.error("[ActualChangePctScheduler] 배치 실패: runDate={}", LocalDate.now(), e);
        }
    }
}
