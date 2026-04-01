package com.whyitrose.batch.prediction;

import com.whyitrose.domain.common.Status;
import com.whyitrose.domain.prediction.Prediction;
import com.whyitrose.domain.prediction.PredictionRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class ActualChangePctJobConfig {

    private final ActualChangePctProcessor actualChangePctProcessor;
    private final PredictionRepository predictionRepository;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job actualChangePctJob(JobRepository jobRepository, Step actualChangePctStep) {
        return new JobBuilder("actualChangePctJob", jobRepository)
                .start(actualChangePctStep)
                .build();
    }

    @Bean
    public Step actualChangePctStep(JobRepository jobRepository,
                                    PlatformTransactionManager transactionManager) {
        return new StepBuilder("actualChangePctStep", jobRepository)
                .<Prediction, Prediction>chunk(100, transactionManager)
                .reader(actualChangePctReader())
                .processor(actualChangePctProcessor)
                .writer(actualChangePctWriter())
                .build();
    }

    @Bean
    public JpaCursorItemReader<Prediction> actualChangePctReader() {
        return new JpaCursorItemReaderBuilder<Prediction>()
                .name("actualChangePctReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT p FROM Prediction p " +
                             "WHERE p.actualChangePct IS NULL AND p.status = :status")
                .parameterValues(Map.of("status", Status.ACTIVE))
                .build();
    }

    @Bean
    public ItemWriter<Prediction> actualChangePctWriter() {
        return chunk -> predictionRepository.saveAll(chunk.getItems());
    }
}