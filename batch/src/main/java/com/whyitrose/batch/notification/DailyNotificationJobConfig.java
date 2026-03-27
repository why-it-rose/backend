package com.whyitrose.batch.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;

@Configuration
@RequiredArgsConstructor
public class DailyNotificationJobConfig {

    private final DigestGenerationStep digestGenerationStep;

    @Bean
    public Job dailyNotificationJob(JobRepository jobRepository, Step digestGenerationJobStep) {
        return new JobBuilder("dailyNotificationJob", jobRepository)
                // 같은 날 재실행 → 같은 JobInstance → 실패한 Step만 재시작
                // 다음 날 실행 → 새 JobInstance → 처음부터
                .incrementer(params -> new JobParametersBuilder()
                        .addLocalDate("runDate", LocalDate.now())
                        .toJobParameters())
                .start(digestGenerationJobStep)
                // NotificationCreationStep, FcmSendStep 추가 시 .next(...)로 연결
                .build();
    }

    @Bean
    public Step digestGenerationJobStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("digestGenerationStep", jobRepository)
                .tasklet(digestGenerationStep, transactionManager)
                .build();
    }
}
