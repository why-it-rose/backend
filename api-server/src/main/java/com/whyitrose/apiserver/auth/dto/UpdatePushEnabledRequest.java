package com.whyitrose.apiserver.auth.dto;

import jakarta.validation.constraints.NotNull;

public record UpdatePushEnabledRequest(
        @NotNull Boolean enabled
) {}