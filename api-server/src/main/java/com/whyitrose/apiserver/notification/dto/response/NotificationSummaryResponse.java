package com.whyitrose.apiserver.notification.dto.response;

public record NotificationSummaryResponse(
        Long notificationId,
        String date,
        String stockNames,
        String relativeTime,
        String message,
        boolean isRead
) {
}