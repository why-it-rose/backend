package com.whyitrose.batch.notification;

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
@ConditionalOnProperty(name = "notification.scheduler.enabled", havingValue = "true")
public class NotificationJobScheduler {

    private final JobLauncher jobLauncher;
    @Qualifier("dailyNotificationJob")
    private final Job dailyNotificationJob;

    @Scheduled(cron = "${notification.scheduler.cron:0 0 8 * * *}", zone = "Asia/Seoul")
    public void runDailyNotification() {
        log.info("일일 알림 스케줄 시작: runDate={}", LocalDate.now());

        JobParameters params = new JobParametersBuilder()
                .addLocalDate("runDate", LocalDate.now())
                .toJobParameters();

        try {
            JobExecution execution = jobLauncher.run(dailyNotificationJob, params);
            log.info("일일 알림 완료: status={}, runDate={}", execution.getStatus(), LocalDate.now());
        } catch (JobInstanceAlreadyCompleteException e) {
            log.info("일일 알림 이미 완료됨 (오늘 중복 실행): runDate={}", LocalDate.now());
        } catch (Exception e) {
            log.error("일일 알림 실패: runDate={}", LocalDate.now(), e);
        }
    }
}
