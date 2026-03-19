package com.whyitrose.domain.prediction;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PredictionDirection {

    UP("상승"),
    DOWN("하락"),
    SIDEWAYS("횡보");

    private final String description;
}