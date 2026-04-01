package com.whyitrose.batch.notification;

import com.google.firebase.messaging.BatchResponse;
import com.whyitrose.core.fcm.FcmService;
import com.whyitrose.domain.common.Status;
import com.whyitrose.domain.digest.DailyNewsDigestItem;
import com.whyitrose.domain.digest.DailyNewsDigestItemRepository;
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
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class FcmSendStep implements ItemProcessor<Notification, NotificationLog> {

    private static final String TITLE = "오늘의 관심 종목 뉴스가 도착했어요 📈";

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
        stockIdToName = new HashMap<>();
        stockIdToNewsIds = new HashMap<>();
        digestStockIds = new HashSet<>();

        // DigestGenerationStep이 jobExecutionContext에 저장한 digestId 사용
        long digestId = stepExecution.getJobExecution().getExecutionContext().getLong("digestId", 0L);
        if (digestId == 0L) {
            log.info("no digestId in jobExecutionContext for {}, skipping", today);
            shouldSkip = true;
            return;
        }

        if (notificationLogRepository.existsByNotification_DigestId(digestId)) {
            log.info("fcm already sent for digestId={}, skipping", digestId);
            shouldSkip = true;
            return;
        }

        List<DailyNewsDigestItem> items = dailyNewsDigestItemRepository
                .findByDigestIdWithStockAndNews(digestId);

        for (DailyNewsDigestItem item : items) {
            Long stockId = item.getStock().getId();
            stockIdToName.put(stockId, item.getStock().getName());
            stockIdToNewsIds.computeIfAbsent(stockId, k -> new HashSet<>())
                    .add(item.getNews().getId());
        }
        digestStockIds = stockIdToName.keySet();
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

            // 개별 응답 확인 — 만료/무효 토큰 삭제
            List<com.google.firebase.messaging.SendResponse> responses = response.getResponses();
            for (int i = 0; i < responses.size(); i++) {
                com.google.firebase.messaging.SendResponse sendResponse = responses.get(i);
                if (!sendResponse.isSuccessful()) {
                    com.google.firebase.messaging.FirebaseMessagingException ex = sendResponse.getException();
                    // UNREGISTERED: 앱 삭제 등으로 토큰이 만료된 경우에만 삭제
                    // INVALID_ARGUMENT는 메시지 형식 문제일 수 있으므로 토큰 삭제 대상에서 제외
                    if (ex != null &&
                            ex.getMessagingErrorCode() == com.google.firebase.messaging.MessagingErrorCode.UNREGISTERED) {
                        String expiredToken = tokenStrings.get(i);
                        log.info("만료된 FCM 토큰 삭제 — userId={}", notification.getUser().getId());
                        fcmTokenRepository.deleteByToken(expiredToken);
                    }
                }
            }

            NotificationLog notificationLog = NotificationLog.create(notification,
                    String.format("{\"successCount\": %d, \"failureCount\": %d}",
                            response.getSuccessCount(), response.getFailureCount()));

            // 1건이라도 성공하면 발송 완료로 처리
            if (response.getSuccessCount() > 0) {
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
