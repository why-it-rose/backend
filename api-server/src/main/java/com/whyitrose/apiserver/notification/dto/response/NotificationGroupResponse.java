package com.whyitrose.apiserver.notification.dto.response;

import java.util.List;

public record NotificationGroupResponse(
        String date,
        Long notificationId,
        boolean isRead,
        List<StockNewsGroup> stocks
) {
}
