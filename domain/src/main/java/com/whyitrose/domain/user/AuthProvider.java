package com.whyitrose.domain.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthProvider {

    EMAIL("이메일");

    private final String description;
}