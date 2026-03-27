package com.whyitrose.batch.notification;

import com.whyitrose.domain.common.Status;
import com.whyitrose.domain.notification.Notification;
import com.whyitrose.domain.notification.NotificationRepository;
import com.whyitrose.domain.user.User;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class DailyNotificationJobConfig {

    private final DigestGenerationStep digestGenerationStep;
    private final NotificationCreationStep notificationCreationStep;
    private final NotificationRepository notificationRepository;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job dailyNotificationJob(JobRepository jobRepository,
                                    Step digestGenerationJobStep,
                                    Step notificationCreationJobStep) {
        return new JobBuilder("dailyNotificationJob", jobRepository)
                // 같은 날 재실행 → 같은 JobInstance → 실패한 Step만 재시작
                // 다음 날 실행 → 새 JobInstance → 처음부터
                .incrementer(params -> new JobParametersBuilder()
                        .addLocalDate("runDate", LocalDate.now())
                        .toJobParameters())
                .start(digestGenerationJobStep)
                .next(notificationCreationJobStep)
                // FcmSendStep 추가 시 .next(...)로 연결
                .build();
    }

    @Bean
    public Step digestGenerationJobStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("digestGenerationStep", jobRepository)
                .tasklet(digestGenerationStep, transactionManager)
                .build();
    }

    @Bean
    public Step notificationCreationJobStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("notificationCreationStep", jobRepository)
                .<User, Notification>chunk(100, transactionManager)
                .reader(userItemReader())
                .processor(notificationCreationStep)
                .writer(chunk -> notificationRepository.saveAll(chunk.getItems()))
                .listener(notificationCreationStep)
                .build();
    }

    @Bean
    public JpaPagingItemReader<User> userItemReader() {
        return new JpaPagingItemReaderBuilder<User>()
                .name("userItemReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT u FROM User u WHERE u.pushEnabled = true AND u.status = :status")
                .parameterValues(Map.of("status", Status.ACTIVE))
                .pageSize(100)
                .build();
    }
}