package com.whyitrose.apiserver.notification.dto.response;

import java.util.List;

public record NotificationNewsItem(
        Long newsId,
        String title,
        String summary,
        String publishedAt,
        String source,
        String url,
        List<String> tags
) {
}
