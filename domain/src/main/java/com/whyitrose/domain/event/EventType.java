package com.whyitrose.domain.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventType {

    SURGE("급등"),
    DROP("급락");

    private final String description;
}