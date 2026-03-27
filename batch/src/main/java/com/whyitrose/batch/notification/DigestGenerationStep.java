package com.whyitrose.batch.notification;

import com.whyitrose.domain.digest.DailyNewsDigest;
import com.whyitrose.domain.digest.DailyNewsDigestItem;
import com.whyitrose.domain.digest.DailyNewsDigestItemRepository;
import com.whyitrose.domain.digest.DailyNewsDigestRepository;
import com.whyitrose.domain.news.NewsStock;
import com.whyitrose.domain.news.NewsStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DigestGenerationStep implements Tasklet {

    private final NewsStockRepository newsStockRepository;
    private final DailyNewsDigestRepository dailyNewsDigestRepository;
    private final DailyNewsDigestItemRepository dailyNewsDigestItemRepository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        LocalDate today = LocalDate.now();
        log.info("DigestGenerationStep started - date={}", today);

        if (dailyNewsDigestRepository.existsByDigestDate(today)) {
            log.info("digest already exists for {}, skipping", today);
            return RepeatStatus.FINISHED;
        }

        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        List<NewsStock> newsStocks = newsStockRepository.findAllWithNewsByPublishedAtBetween(startOfDay, endOfDay);

        if (newsStocks.isEmpty()) {
            log.info("no news found for {}, skipping digest creation", today);
            return RepeatStatus.FINISHED;
        }

        DailyNewsDigest digest = DailyNewsDigest.create(today);
        dailyNewsDigestRepository.save(digest);

        List<DailyNewsDigestItem> items = newsStocks.stream()
                .map(ns -> DailyNewsDigestItem.create(digest, ns.getNews(), ns.getStock()))
                .collect(Collectors.toList());
        dailyNewsDigestItemRepository.saveAll(items);

        int distinctNewsCount = (int) newsStocks.stream()
                .map(ns -> ns.getNews().getId())
                .distinct()
                .count();
        digest.activate(distinctNewsCount);
        dailyNewsDigestRepository.save(digest);

        log.info("digest created - date={}, newsCount={}, itemCount={}", today, distinctNewsCount, items.size());

        return RepeatStatus.FINISHED;
    }
}
