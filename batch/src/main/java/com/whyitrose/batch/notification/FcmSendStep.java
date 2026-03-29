package com.whyitrose.batch.notification;

import com.google.firebase.messaging.BatchResponse;
import com.whyitrose.core.fcm.FcmService;
import com.whyitrose.domain.common.Status;
import com.whyitrose.domain.digest.DailyNewsDigest;
import com.whyitrose.domain.digest.DailyNewsDigestItem;
import com.whyitrose.domain.digest.DailyNewsDigestItemRepository;
import com.whyitrose.domain.digest.DailyNewsDigestRepository;
import com.whyitrose.domain.fcm.FcmToken;
import com.whyitrose.domain.fcm.FcmTokenRepository;
import com.whyitrose.domain.interest.InterestStock;
import com.whyitrose.domain.interest.InterestStockRepository;
import com.whyitrose.domain.notification.Notification;
import com.whyitrose.domain.notification.NotificationLog;
import com.whyitrose.domain.notification.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class FcmSendStep implements ItemProcessor<Notification, NotificationLog> {

    private static final String TITLE = "오늘의 관심 종목 뉴스가 도착했어요 📈";

    private final DailyNewsDigestRepository dailyNewsDigestRepository;
    private final DailyNewsDigestItemRepository dailyNewsDigestItemRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final InterestStockRepository interestStockRepository;
    private final FcmService fcmService;

    private boolean shouldSkip;
    private int successCount;
    private int failureCount;
    private Set<Long> digestStockIds;
    private Map<Long, String> stockIdToName;
    private Map<Long, Set<Long>> stockIdToNewsIds;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        LocalDate today = LocalDate.now();
        log.info("FcmSendStep started - date={}", today);
        shouldSkip = false;
        successCount = 0;
        failureCount = 0;

        Optional<DailyNewsDigest> digestOpt = dailyNewsDigestRepository.findByDigestDate(today);
        if (digestOpt.isEmpty()) {
            log.info("no digest found for {}, skipping", today);
            shouldSkip = true;
            stepExecution.getExecutionContext().putLong("digestId", 0L);
            return;
        }

        DailyNewsDigest digest = digestOpt.get();

        if (notificationLogRepository.existsByNotification_DigestId(digest.getId())) {
            log.info("fcm already sent for {}, skipping", today);
            shouldSkip = true;
            stepExecution.getExecutionContext().putLong("digestId", 0L);
            return;
        }

        List<DailyNewsDigestItem> items = dailyNewsDigestItemRepository
                .findByDigestIdWithStockAndNews(digest.getId());

        stockIdToName = new HashMap<>();
        stockIdToNewsIds = new HashMap<>();
        for (DailyNewsDigestItem item : items) {
            Long stockId = item.getStock().getId();
            stockIdToName.put(stockId, item.getStock().getName());
            stockIdToNewsIds.computeIfAbsent(stockId, k -> new HashSet<>())
                    .add(item.getNews().getId());
        }
        digestStockIds = stockIdToName.keySet();

        stepExecution.getExecutionContext().putLong("digestId", digest.getId());
    }

    @Override
    public NotificationLog process(Notification notification) {
        if (shouldSkip) {
            return null;
        }

        List<FcmToken> tokens = fcmTokenRepository.findAllByUserId(notification.getUser().getId());

        if (tokens.isEmpty()) {
            log.info("no fcm token for userId={}, logging failure", notification.getUser().getId());
            failureCount++;
            NotificationLog notificationLog = NotificationLog.create(notification,
                    "{\"reason\": \"no_fcm_token\"}");
            notificationLog.markAsFailed();
            return notificationLog;
        }

        List<String> tokenStrings = tokens.stream()
                .map(FcmToken::getToken)
                .toList();

        String body = buildBody(notification.getUser().getId());

        try {
            BatchResponse response = fcmService.sendMulticast(tokenStrings, TITLE, body);
            NotificationLog notificationLog = NotificationLog.create(notification,
                    String.format("{\"successCount\": %d, \"failureCount\": %d}",
                            response.getSuccessCount(), response.getFailureCount()));

            if (response.getFailureCount() == 0) {
                notificationLog.markAsSent();
                successCount++;
            } else {
                notificationLog.markAsFailed();
                failureCount++;
            }
            return notificationLog;

        } catch (Exception e) {
            failureCount++;
            NotificationLog notificationLog = NotificationLog.create(notification,
                    String.format("{\"error\": \"%s\"}", e.getMessage()));
            notificationLog.markAsFailed();
            return notificationLog;
        }
    }

    @AfterStep
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (!shouldSkip) {
            log.info("FcmSendStep completed - date={}, successCount={}, failureCount={}",
                    LocalDate.now(), successCount, failureCount);
        }
        return stepExecution.getExitStatus();
    }

    private String buildBody(Long userId) {
        List<InterestStock> interestStocks = interestStockRepository
                .findByUserIdAndStatus(userId, Status.ACTIVE);

        List<Long> matchedStockIds = interestStocks.stream()
                .map(is -> is.getStock().getId())
                .filter(digestStockIds::contains)
                .toList();

        if (matchedStockIds.isEmpty()) {
            return "왜 올랐지?에서 오늘의 뉴스를 확인해보세요";
        }

        long totalNewsCount = matchedStockIds.stream()
                .flatMap(id -> stockIdToNewsIds.getOrDefault(id, Set.of()).stream())
                .distinct()
                .count();

        String firstName = stockIdToName.get(matchedStockIds.get(0));
        int stockCount = matchedStockIds.size();

        if (stockCount == 1) {
            return firstName + ", 총 " + totalNewsCount + "건의 뉴스를 확인해보세요";
        }
        return firstName + " 외 " + (stockCount - 1) + "개 종목, 총 " + totalNewsCount + "건의 뉴스를 확인해보세요";
    }
}
