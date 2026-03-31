package com.whyitrose.apiserver.scrap.dto;

import java.util.List;

public record MyScrapSearchPageResponse(
        List<MyScrapSearchItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
