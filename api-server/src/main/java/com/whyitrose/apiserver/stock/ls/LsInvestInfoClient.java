package com.whyitrose.apiserver.stock.ls;

public interface LsInvestInfoClient {

    LsCompanyInfo fetchCompanyInfo(String ticker);
}
