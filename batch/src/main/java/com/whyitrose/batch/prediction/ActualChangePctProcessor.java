package com.whyitrose.batch.prediction;

import com.whyitrose.domain.prediction.Prediction;
import com.whyitrose.domain.stock.StockPrice;
import com.whyitrose.domain.stock.StockPricePeriod;
import com.whyitrose.domain.stock.StockPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActualChangePctProcessor implements ItemProcessor<Prediction, Prediction> {

    private final StockPriceRepository stockPriceRepository;

    @Override
    public Prediction process(Prediction prediction) {
        LocalDate digestDate = prediction.getDigest().getDigestDate();
        Long stockId = prediction.getStock().getId();

        Optional<StockPrice> todayPrice = stockPriceRepository
                .findByStockIdAndTradingDateAndPeriod(stockId, digestDate, StockPricePeriod.DAILY);

        // 전일 주가: -1일부터 역방향으로 가장 가까운 거래일 (주말/공휴일 대응)
        Optional<StockPrice> prevPrice = stockPriceRepository
                .findTopByStockIdAndPeriodAndTradingDateLessThanEqualOrderByTradingDateDesc(
                        stockId, StockPricePeriod.DAILY, digestDate.minusDays(1));

        if (todayPrice.isEmpty() || prevPrice.isEmpty()) {
            log.warn("[ActualChangePctProcessor] 주가 데이터 없음 - predictionId: {}, date: {}",
                    prediction.getId(), digestDate);
            return null; // Spring Batch: null 반환 시 해당 아이템 스킵
        }

        BigDecimal todayClose = BigDecimal.valueOf(todayPrice.get().getClosePrice());
        BigDecimal prevClose = BigDecimal.valueOf(prevPrice.get().getClosePrice());

        BigDecimal changePct = todayClose.subtract(prevClose)
                .divide(prevClose, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        prediction.review(changePct);
        return prediction;
    }
}
