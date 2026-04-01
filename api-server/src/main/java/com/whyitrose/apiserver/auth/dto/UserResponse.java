package com.whyitrose.apiserver.auth.dto;

import com.whyitrose.domain.common.Status;
import com.whyitrose.domain.user.AuthProvider;
import com.whyitrose.domain.user.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String name,
        String nickname,
        AuthProvider provider,
        boolean pushEnabled,
        boolean marketingAgreed,
        Status status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    // Entity를 DTO로 변환
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getProvider(),
                user.isPushEnabled(),
                user.isMarketingAgreed(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
