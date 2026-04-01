package com.whyitrose.apiserver.me.dto;

import jakarta.validation.constraints.NotNull;

public record AddInterestStockRequest(
        @NotNull Long stockId
) {}
