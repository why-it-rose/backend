package com.whyitrose.apiserver.auth.dto;

public record LoginResponse(
        Long userId,
        String email,
        String nickname,
        String accessToken,
        String refreshToken
) {}
