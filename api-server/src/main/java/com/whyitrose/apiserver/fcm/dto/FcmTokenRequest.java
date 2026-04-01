package com.whyitrose.apiserver.fcm.dto;

import jakarta.validation.constraints.NotBlank;

public record FcmTokenRequest(
        @NotBlank String token
) {}
