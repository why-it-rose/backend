package com.whyitrose.apiserver.auth.dto;

public record LoginResponse(
        Long userId,
        String email,
        String nickname,
        boolean pushEnabled
) {}
