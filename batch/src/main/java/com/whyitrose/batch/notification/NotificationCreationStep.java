package com.whyitrose.batch.notification;

import com.whyitrose.domain.common.Status;
import com.whyitrose.domain.digest.DailyNewsDigest;
import com.whyitrose.domain.digest.DailyNewsDigestItem;
import com.whyitrose.domain.digest.DailyNewsDigestItemRepository;
import com.whyitrose.domain.digest.DailyNewsDigestRepository;
import com.whyitrose.domain.interest.InterestStock;
import com.whyitrose.domain.interest.InterestStockRepository;
import com.whyitrose.domain.notification.Notification;
import com.whyitrose.domain.notification.NotificationRepository;
import com.whyitrose.domain.notification.NotificationType;
import com.whyitrose.domain.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCreationStep implements ItemProcessor<User, Notification> {

    private final DailyNewsDigestRepository dailyNewsDigestRepository;
    private final DailyNewsDigestItemRepository dailyNewsDigestItemRepository;
    private final NotificationRepository notificationRepository;
    private final InterestStockRepository interestStockRepository;

    private DailyNewsDigest todayDigest;
    private Set<Long> digestStockIds;
    private boolean shouldSkip;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        LocalDate today = LocalDate.now();
        log.info("NotificationCreationStep started - date={}", today);
        shouldSkip = false;
        todayDigest = null;
        digestStockIds = null;

        Optional<DailyNewsDigest> digestOpt = dailyNewsDigestRepository.findByDigestDate(today);
        if (digestOpt.isEmpty()) {
            log.info("no digest found for {}, skipping", today);
            shouldSkip = true;
            return;
        }

        todayDigest = digestOpt.get();

        if (notificationRepository.existsByDigestId(todayDigest.getId())) {
            log.info("notifications already created for {}, skipping", today);
            shouldSkip = true;
            return;
        }

        List<DailyNewsDigestItem> items = dailyNewsDigestItemRepository
                .findByDigestIdAndStatus(todayDigest.getId(), Status.ACTIVE);
        digestStockIds = items.stream()
                .map(item -> item.getStock().getId())
                .collect(Collectors.toSet());
    }

    @Override
    public Notification process(User user) {
        if (shouldSkip) {
            return null;
        }

        List<InterestStock> interestStocks = interestStockRepository
                .findByUserIdAndStatus(user.getId(), Status.ACTIVE);

        boolean hasMatchingStock = interestStocks.stream()
                .anyMatch(is -> digestStockIds.contains(is.getStock().getId()));

        if (!hasMatchingStock) {
            return null;
        }

        return Notification.create(user, NotificationType.NEWS, todayDigest);
    }

    @AfterStep
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (!shouldSkip) {
            log.info("notifications created - date={}, count={}", LocalDate.now(), stepExecution.getWriteCount());
        }
        return stepExecution.getExitStatus();
    }
}