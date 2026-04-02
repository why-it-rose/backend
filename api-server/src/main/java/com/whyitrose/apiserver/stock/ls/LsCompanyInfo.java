package com.whyitrose.apiserver.stock.ls;

public record LsCompanyInfo(
        String industryGroup,
        long marketCap,
        long totalShares,
        double foreignRatio
) {}
