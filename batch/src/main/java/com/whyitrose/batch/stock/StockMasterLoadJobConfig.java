package com.whyitrose.batch.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StockMasterLoadJobConfig {

    private final StockMasterLoadService stockMasterLoadService;

    @Bean
    public Job stockMasterLoadJob(JobRepository jobRepository, Step stockMasterLoadStep) {
        return new JobBuilder("stockMasterLoadJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(stockMasterLoadStep)
                .build();
    }

    @Bean
    public Step stockMasterLoadStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("stockMasterLoadStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    stockMasterLoadService.loadAndUpsert();
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}