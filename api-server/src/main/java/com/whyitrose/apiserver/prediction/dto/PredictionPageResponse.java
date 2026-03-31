package com.whyitrose.apiserver.prediction.dto;

import java.util.List;

public record PredictionPageResponse(
        List<PredictionGroupResponse> groups,
        Long nextCursor,
        boolean hasNext
) {}
