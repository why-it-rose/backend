package com.whyitrose.apiserver.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateMeRequest(
        @NotBlank String nickname
) {}
