package com.whyitrose.batch.event;

import com.whyitrose.domain.event.Event;
import com.whyitrose.domain.event.EventRepository;
import com.whyitrose.domain.event.EventType;
import com.whyitrose.domain.stock.Stock;
import com.whyitrose.domain.stock.StockPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSaveService {

    private final EventRepository eventRepository;
    private final StockPriceRepository stockPriceRepository;

    /**
     * 이벤트 저장 — 병합 또는 신규 생성
     * 1. 직전 거래일 조회
     * 2. 병합 대상 이벤트 존재하면 extend()
     * 3. 없으면 새 이벤트 생성
     *
     * @return 저장 성공 여부
     */
    @Transactional
    public boolean saveEvent(Stock stock, EventType eventType,
                             LocalDate targetDate, int priceBefore, int priceAfter,
                             BigDecimal changePct) {

        // 직전 거래일 조회
        Optional<LocalDate> prevTradingDate = stockPriceRepository
                .findRecentPricesBeforeDate(stock.getId(), targetDate, PageRequest.of(0, 1))
                .stream()
                .map(sp -> sp.getTradingDate())
                .findFirst();

        // 병합 대상 이벤트 조회
        Optional<Event> mergeable = prevTradingDate.flatMap(prevDate ->
                eventRepository.findMergeable(stock.getId(), eventType, prevDate));

        if (mergeable.isPresent()) {
            // 병합 — endDate, priceAfter, changePct 업데이트
            Event event = mergeable.get();
            BigDecimal mergedChangePct = calculateChangePct(event.getPriceBefore(), priceAfter);
            event.extend(targetDate, priceAfter, mergedChangePct);

            log.info("[EventDetection] 이벤트 병합 — ticker={}, type={}, startDate={}, endDate={}, changePct={}%",
                    stock.getTicker(), eventType, event.getStartDate(), targetDate, mergedChangePct);
            return true;
        }

        // 동일 stock_id + start_date 중복 방지
        if (eventRepository.existsByStockIdAndStartDate(stock.getId(), targetDate)) {
            log.debug("[EventDetection] 이미 존재하는 이벤트 — ticker={}, date={}", stock.getTicker(), targetDate);
            return false;
        }

        // 신규 이벤트 생성
        Event event = Event.create(stock, eventType, targetDate, targetDate,
                changePct, priceBefore, priceAfter);
        eventRepository.save(event);

        log.info("[EventDetection] 이벤트 저장 — ticker={}, type={}, date={}, changePct={}%",
                stock.getTicker(), eventType, targetDate, changePct);
        return true;
    }

    private BigDecimal calculateChangePct(int priceBefore, int priceAfter) {
        return BigDecimal.valueOf(priceAfter - priceBefore)
                .divide(BigDecimal.valueOf(priceBefore), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
