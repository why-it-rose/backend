package com.whyitrose.domain.stock;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StockMarket {

    KOSPI("코스피"),
    KOSDAQ("코스닥"),
    KONEX("코넥스");

    private final String description;
}