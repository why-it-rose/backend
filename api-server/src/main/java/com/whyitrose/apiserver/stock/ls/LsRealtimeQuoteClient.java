package com.whyitrose.apiserver.stock.ls;

public interface LsRealtimeQuoteClient {

    LsRealtimeQuote fetchQuote(String shcode);
}
