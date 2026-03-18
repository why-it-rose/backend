package com.whyitrose.apiserver.example.post.dto;

import jakarta.validation.constraints.NotBlank;

public record PostCreateRequest(
        @NotBlank String title,
        @NotBlank String content
) {
}