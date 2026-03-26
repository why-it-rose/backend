package com.whyitrose.apiserver.auth.jwt;

public record JwtClaims(
        Long userId,
        String tokenType
) {}
