package com.whyitrose.batch.stock;

import com.whyitrose.domain.stock.Stock;
import com.whyitrose.domain.stock.StockPrice;
import com.whyitrose.domain.stock.StockPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockPriceItemWriter implements ItemWriter<StockPriceBatch> {

    private final StockPriceRepository stockPriceRepository;

    @Override
    public void write(Chunk<? extends StockPriceBatch> chunk) {
        for (StockPriceBatch batch : chunk) {
            Stock stock = batch.stock();
            for (StockPriceBatch.PriceRow row : batch.rows()) {
                upsert(stock, row);
            }
            log.info("주가 적재 완료: ticker={}, rows={}", stock.getTicker(), batch.rows().size());
        }
    }

    private void upsert(Stock stock, StockPriceBatch.PriceRow row) {
        stockPriceRepository
                .findByStockIdAndTradingDateAndPeriod(stock.getId(), row.tradingDate(), row.period())
                .map(existing -> {
                    existing.applyPrice(row.open(), row.close(), row.high(), row.low(), row.volume());
                    return stockPriceRepository.save(existing);
                })
                .orElseGet(() -> stockPriceRepository.save(
                        StockPrice.create(stock, row.tradingDate(), row.period(),
                                row.open(), row.close(), row.high(), row.low(), row.volume())));
    }
}