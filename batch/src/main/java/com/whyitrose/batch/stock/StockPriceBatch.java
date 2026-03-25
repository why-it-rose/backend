package com.whyitrose.batch.stock;

import com.whyitrose.domain.stock.Stock;
import com.whyitrose.domain.stock.StockPricePeriod;
import java.time.LocalDate;
import java.util.List;

record StockPriceBatch(Stock stock, List<PriceRow> rows) {

    record PriceRow(
            LocalDate tradingDate,
            StockPricePeriod period,
            int open,
            int close,
            int high,
            int low,
            long volume) {}
}