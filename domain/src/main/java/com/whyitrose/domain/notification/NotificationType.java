package com.whyitrose.domain.notification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {

    NEWS("뉴스"),
    EVENT("이벤트"),
    REVIEW("복기"),
    SYSTEM("시스템");

    private final String description;
}