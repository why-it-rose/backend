package com.whyitrose.batch.stock;

import com.whyitrose.domain.common.Status;
import com.whyitrose.domain.stock.Stock;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StockPriceLoadJobConfig {

    private final StockPricePartitioner partitioner;
    private final StockPriceItemProcessor processor;
    private final StockPriceItemWriter writer;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job stockPriceLoadJob(JobRepository jobRepository, Step stockPriceMasterStep) {
        return new JobBuilder("stockPriceLoadJob", jobRepository)
                // 같은 날 재실행 → 같은 JobInstance → 실패한 파티션만 재시작
                // 다음 날 실행 → 새 JobInstance → 처음부터
                .incrementer(params -> new JobParametersBuilder()
                        .addLocalDate("runDate", LocalDate.now())
                        .toJobParameters())
                .start(stockPriceMasterStep)
                .build();
    }

    @Bean
    public Step stockPriceMasterStep(JobRepository jobRepository, Step stockPriceWorkerStep,
            @Value("${stock.price.thread-count:4}") int threadCount) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadCount);
        executor.setMaxPoolSize(threadCount);
        executor.setThreadNamePrefix("stock-price-");
        executor.initialize();

        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
        handler.setStep(stockPriceWorkerStep);
        handler.setTaskExecutor(executor);
        handler.setGridSize(threadCount);

        return new StepBuilder("stockPriceMasterStep", jobRepository)
                .partitioner("stockPriceWorkerStep", partitioner)
                .partitionHandler(handler)
                .build();
    }

    @Bean
    public Step stockPriceWorkerStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            JpaPagingItemReader<Stock> stockPriceWorkerReader) {
        return new StepBuilder("stockPriceWorkerStep", jobRepository)
                .<Stock, StockPriceBatch>chunk(1, transactionManager)
                .reader(stockPriceWorkerReader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Stock> stockPriceWorkerReader(
            @Value("#{stepExecutionContext['startId']}") Long startId,
            @Value("#{stepExecutionContext['endId']}") Long endId) {
        return new JpaPagingItemReaderBuilder<Stock>()
                .name("stockPriceWorkerReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT s FROM Stock s WHERE s.status = :status"
                        + " AND s.id >= :startId AND s.id <= :endId ORDER BY s.id ASC")
                .parameterValues(Map.of("status", Status.ACTIVE, "startId", startId, "endId", endId))
                .pageSize(10)
                .build();
    }
}