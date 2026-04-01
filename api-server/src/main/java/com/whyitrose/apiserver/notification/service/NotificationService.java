package com.whyitrose.apiserver.notification.service;

import com.whyitrose.apiserver.notification.dto.response.NotificationGroupResponse;
import com.whyitrose.apiserver.notification.dto.response.NotificationNewsItem;
import com.whyitrose.apiserver.notification.dto.response.NotificationSummaryResponse;
import com.whyitrose.apiserver.notification.dto.response.StockNewsGroup;
import com.whyitrose.apiserver.notification.dto.response.UnreadCountResponse;
import com.whyitrose.core.exception.BaseException;
import com.whyitrose.core.response.BaseResponseStatus;
import com.whyitrose.domain.common.Status;
import com.whyitrose.domain.digest.DailyNewsDigestItem;
import com.whyitrose.domain.digest.DailyNewsDigestItemRepository;
import com.whyitrose.domain.interest.InterestStockRepository;
import com.whyitrose.domain.news.News;
import com.whyitrose.domain.news.NewsTagRepository;
import com.whyitrose.domain.notification.Notification;
import com.whyitrose.domain.notification.NotificationRepository;
import com.whyitrose.domain.stock.Stock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter PUBLISHED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final NotificationRepository notificationRepository;
    private final DailyNewsDigestItemRepository digestItemRepository;
    private final InterestStockRepository interestStockRepository;
    private final NewsTagRepository newsTagRepository;

    // ==================== 공개 API ====================

    public List<NotificationGroupResponse> getNotifications(Long userId, Integer days, Long stockId, Boolean read) {
        List<Notification> notifications = fetchNotifications(userId, days);

        if (read != null) {
            notifications = notifications.stream()
                    .filter(n -> read ? n.isRead() : !n.isRead())
                    .toList();
        }

        if (notifications.isEmpty()) {
            return List.of();
        }

        Set<Long> interestStockIds = fetchInterestStockIds(userId);
        Map<Long, List<DailyNewsDigestItem>> itemsByDigestId =
                buildItemsByDigestId(notifications, interestStockIds);

        // 세부 알림 전용: stockId 필터 추가 적용
        if (stockId != null) {
            itemsByDigestId = itemsByDigestId.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().stream()
                                    .filter(item -> item.getStock().getId().equals(stockId))
                                    .toList()
                    ));
        }

        // 태그 조회 (세부 알림 전용)
        List<Long> newsIds = itemsByDigestId.values().stream()
                .flatMap(List::stream)
                .map(item -> item.getNews().getId())
                .distinct()
                .toList();

        Map<Long, List<String>> tagsByNewsId = newsIds.isEmpty()
                ? Map.of()
                : newsTagRepository.findByNewsIdInWithTag(newsIds).stream()
                        .collect(Collectors.groupingBy(
                                nt -> nt.getNews().getId(),
                                Collectors.mapping(nt -> nt.getTag().getName(), Collectors.toList())
                        ));

        Map<Long, List<DailyNewsDigestItem>> finalItemsByDigestId = itemsByDigestId;
        return notifications.stream()
                .map(n -> toGroupResponse(n, finalItemsByDigestId, tagsByNewsId))
                .filter(group -> !group.stocks().isEmpty())
                .toList();
    }

    public List<NotificationSummaryResponse> getSummary(Long userId, Integer days) {
        List<Notification> notifications = fetchNotifications(userId, days);

        if (notifications.isEmpty()) {
            return List.of();
        }

        Set<Long> interestStockIds = fetchInterestStockIds(userId);
        Map<Long, List<DailyNewsDigestItem>> itemsByDigestId =
                buildItemsByDigestId(notifications, interestStockIds);

        LocalDate today = LocalDate.now();

        return notifications.stream()
                .map(n -> toSummaryResponse(n, itemsByDigestId, today))
                .filter(s -> s != null)
                .toList();
    }

    public UnreadCountResponse getUnreadCount(Long userId) {
        long count = notificationRepository.countByUserIdAndReadAtIsNullAndStatus(userId, Status.ACTIVE);
        return new UnreadCountResponse(count);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId, LocalDateTime.now());
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.NOTIFICATION_NOT_FOUND));

        if (!notification.getUser().getId().equals(userId)) {
            throw new BaseException(BaseResponseStatus.NOTIFICATION_FORBIDDEN);
        }

        if (!notification.isRead()) {
            notification.markAsRead();
        }
    }

    // ==================== 공통 private 메서드 ====================

    private List<Notification> fetchNotifications(Long userId, Integer days) {
        if (days != null) {
            return notificationRepository.findByUserIdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
                    userId, Status.ACTIVE, LocalDateTime.now().minusDays(days));
        }
        return notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, Status.ACTIVE);
    }

    private Set<Long> fetchInterestStockIds(Long userId) {
        return interestStockRepository.findByUserIdAndStatus(userId, Status.ACTIVE)
                .stream()
                .map(is -> is.getStock().getId())
                .collect(Collectors.toSet());
    }

    /**
     * digestId IN절로 items 한번에 조회 후 관심종목 필터 적용, digestId 기준 Map 반환
     */
    private Map<Long, List<DailyNewsDigestItem>> buildItemsByDigestId(
            List<Notification> notifications,
            Set<Long> interestStockIds
    ) {
        List<Long> digestIds = notifications.stream()
                .filter(n -> n.getDigest() != null)
                .map(n -> n.getDigest().getId())
                .distinct()
                .toList();

        if (digestIds.isEmpty()) {
            return Map.of();
        }

        return digestItemRepository.findByDigestIdInWithStockAndNews(digestIds).stream()
                .filter(item -> interestStockIds.contains(item.getStock().getId()))
                .collect(Collectors.groupingBy(item -> item.getDigest().getId()));
    }

    // ==================== 세부 알림 조립 ====================

    private NotificationGroupResponse toGroupResponse(
            Notification notification,
            Map<Long, List<DailyNewsDigestItem>> itemsByDigestId,
            Map<Long, List<String>> tagsByNewsId
    ) {
        List<DailyNewsDigestItem> items = notification.getDigest() != null
                ? itemsByDigestId.getOrDefault(notification.getDigest().getId(), List.of())
                : List.of();

        Map<Long, List<DailyNewsDigestItem>> itemsByStock = items.stream()
                .collect(Collectors.groupingBy(item -> item.getStock().getId()));

        List<StockNewsGroup> stockGroups = itemsByStock.values().stream()
                .map(stockItems -> toStockNewsGroup(stockItems, tagsByNewsId))
                .toList();

        return new NotificationGroupResponse(
                notification.getCreatedAt().format(DATE_FORMATTER),
                notification.getId(),
                notification.isRead(),
                stockGroups
        );
    }

    private StockNewsGroup toStockNewsGroup(
            List<DailyNewsDigestItem> stockItems,
            Map<Long, List<String>> tagsByNewsId
    ) {
        Stock stock = stockItems.get(0).getStock();

        List<NotificationNewsItem> newsList = stockItems.stream()
                .map(item -> toNewsItem(item.getNews(), tagsByNewsId))
                .toList();

        return new StockNewsGroup(
                stock.getId(),
                stock.getName(),
                stock.getTicker(),
                stock.getLogoUrl(),
                newsList.size(),
                newsList
        );
    }

    private NotificationNewsItem toNewsItem(News news, Map<Long, List<String>> tagsByNewsId) {
        String content = news.getContent();
        String summary = content.length() > 100 ? content.substring(0, 100) : content;

        return new NotificationNewsItem(
                news.getId(),
                news.getTitle(),
                summary,
                news.getPublishedAt().format(PUBLISHED_AT_FORMATTER),
                news.getSource(),
                news.getUrl(),
                tagsByNewsId.getOrDefault(news.getId(), List.of())
        );
    }

    // ==================== 요약 알림 조립 ====================

    private NotificationSummaryResponse toSummaryResponse(
            Notification notification,
            Map<Long, List<DailyNewsDigestItem>> itemsByDigestId,
            LocalDate today
    ) {
        List<DailyNewsDigestItem> items = notification.getDigest() != null
                ? itemsByDigestId.getOrDefault(notification.getDigest().getId(), List.of())
                : List.of();

        if (items.isEmpty()) {
            return null;
        }

        List<String> stockNames = items.stream()
                .map(item -> item.getStock().getName())
                .distinct()
                .toList();

        long newsCount = items.stream()
                .map(item -> item.getNews().getId())
                .distinct()
                .count();

        return new NotificationSummaryResponse(
                notification.getId(),
                notification.getCreatedAt().format(DATE_FORMATTER),
                buildStockNames(stockNames),
                buildRelativeTime(notification.getCreatedAt().toLocalDate(), today),
                buildMessage(stockNames, newsCount),
                notification.isRead()
        );
    }

    private String buildStockNames(List<String> stockNames) {
        int total = stockNames.size();
        if (total <= 3) {
            return String.join(", ", stockNames);
        }
        String front = String.join(", ", stockNames.subList(0, 3));
        return front + " 외 " + (total - 3) + "개";
    }

    private String buildRelativeTime(LocalDate notificationDate, LocalDate today) {
        long daysBetween = ChronoUnit.DAYS.between(notificationDate, today);
        if (daysBetween == 0) return "오늘";
        return daysBetween + "일 전";
    }

    private String buildMessage(List<String> stockNames, long newsCount) {
        int total = stockNames.size();

        if (total == 1) {
            return "**" + stockNames.get(0) + "**, 총 **" + newsCount + "**건의 뉴스를 확인해보세요.";
        }

        String boldNames;
        if (total <= 3) {
            boldNames = "**" + String.join(", ", stockNames) + "**";
            return boldNames + ", 총 **" + newsCount + "**건의 뉴스를 확인해보세요.";
        }

        boldNames = "**" + String.join(", ", stockNames.subList(0, 3)) + "**";
        return boldNames + " 포함 총 " + total + " 종목, **" + newsCount + "**건의 뉴스를 확인해보세요.";
    }
}
