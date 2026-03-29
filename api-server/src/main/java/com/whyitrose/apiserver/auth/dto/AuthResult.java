package com.whyitrose.apiserver.auth.dto;

public record AuthResult(
        Long userId,
        String email,
        String nickname,
        String accessToken,
        String refreshToken
) {
    public LoginResponse toLoginResponse() {
        return new LoginResponse(userId, email, nickname);
    }
}