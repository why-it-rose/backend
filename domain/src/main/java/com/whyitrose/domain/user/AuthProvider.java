package com.whyitrose.domain.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthProvider {

    EMAIL("이메일"),
    KAKAO("카카오"),
    GOOGLE("구글");

    private final String description;
}