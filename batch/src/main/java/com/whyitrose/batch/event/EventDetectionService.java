package com.whyitrose.batch.event;

import com.whyitrose.domain.common.Status;
import com.whyitrose.domain.event.Event;
import com.whyitrose.domain.event.EventRepository;
import com.whyitrose.domain.event.EventType;
import com.whyitrose.domain.stock.Stock;
import com.whyitrose.domain.stock.StockPrice;
import com.whyitrose.domain.stock.StockPriceRepository;
import com.whyitrose.domain.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventDetectionService {

    // 급등/급락 임계값 (%)
    private static final BigDecimal SURGE_THRESHOLD = new BigDecimal("5.00");
    private static final BigDecimal DROP_THRESHOLD  = new BigDecimal("-5.00");

    // 거래량 조건 — 최근 N일 평균 대비 배율
    private static final int    VOLUME_LOOKBACK_DAYS   = 20;
    private static final double VOLUME_RATIO_THRESHOLD = 1.5;

    private final StockRepository      stockRepository;
    private final StockPriceRepository stockPriceRepository;
    private final EventRepository      eventRepository;

    /**
     * targetDate 하루치 전체 ACTIVE 종목에 대해 이벤트 탐지 실행
     */
    @Transactional
    public void detectAndSaveEvents(LocalDate targetDate) {
        List<Stock> activeStocks = stockRepository.findByStatus(Status.ACTIVE);
        log.info("[EventDetection] 탐지 시작 — targetDate={}, 대상종목={}개", targetDate, activeStocks.size());

        for (Stock stock : activeStocks) {
            try {
                detectForStock(stock, targetDate);
            } catch (Exception e) {
                log.warn("[EventDetection] 종목 처리 중 오류 — ticker={}, date={}, msg={}",
                        stock.getTicker(), targetDate, e.getMessage());
            }
        }

        log.info("[EventDetection] 탐지 완료 — targetDate={}", targetDate);
    }

    // ── 종목별 단일일 이벤트 탐지 ─────────────────────────────────────

    private void detectForStock(Stock stock, LocalDate targetDate) {
        StockPrice curr = stockPriceRepository
                .findByStockIdAndTradingDate(stock.getId(), targetDate)
                .orElse(null);

        if (curr == null) {
            return; // 해당일 주가 데이터 없음
        }

        // 전일 주가 조회 (targetDate 기준 바로 이전 거래일 1개)
        List<StockPrice> prevList = stockPriceRepository.findRecentPricesBeforeDate(
                stock.getId(), targetDate, PageRequest.of(0, 1));

        if (prevList.isEmpty()) {
            return; // 비교할 전일 데이터 없음
        }

        StockPrice prev = prevList.get(0);

        // ① 변동률 계산
        BigDecimal changePct = calculateChangePct(prev.getClosePrice(), curr.getClosePrice());

        // ② 가격 조건 판단
        EventType eventType = resolveEventType(changePct);
        if (eventType == null) {
            return; // 임계값 미달
        }

        // ③ 거래량 조건 판단
        if (!isVolumeConditionMet(stock.getId(), targetDate, curr.getVolume())) {
            log.debug("[EventDetection] 거래량 조건 미충족 — ticker={}, date={}", stock.getTicker(), targetDate);
            return;
        }

        // ④ 이벤트 저장
        saveEvent(stock, eventType, targetDate, prev.getClosePrice(), curr.getClosePrice(), changePct);
    }

    // ── 변동률 계산 ───────────────────────────────────────────────────

    private BigDecimal calculateChangePct(int priceBefore, int priceAfter) {
        // (priceAfter - priceBefore) / priceBefore × 100
        return BigDecimal.valueOf(priceAfter - priceBefore)
                .divide(BigDecimal.valueOf(priceBefore), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    // ── 이벤트 타입 결정 ──────────────────────────────────────────────

    private EventType resolveEventType(BigDecimal changePct) {
        if (changePct.compareTo(SURGE_THRESHOLD) >= 0) return EventType.SURGE;
        if (changePct.compareTo(DROP_THRESHOLD)  <= 0) return EventType.DROP;
        return null;
    }

    // ── 거래량 조건 검증 ──────────────────────────────────────────────

    private boolean isVolumeConditionMet(Long stockId, LocalDate targetDate, long currentVolume) {
        List<StockPrice> recentPrices = stockPriceRepository.findRecentPricesBeforeDate(
                stockId, targetDate, PageRequest.of(0, VOLUME_LOOKBACK_DAYS));

        if (recentPrices.isEmpty()) {
            return false;
        }

        double avgVolume = recentPrices.stream()
                .mapToLong(StockPrice::getVolume)
                .average()
                .orElse(0);

        return currentVolume >= avgVolume * VOLUME_RATIO_THRESHOLD;
    }

    // ── 이벤트 저장 (중복 방지 포함) ─────────────────────────────────

    @Transactional
    protected void saveEvent(Stock stock, EventType eventType,
                             LocalDate targetDate, int priceBefore, int priceAfter,
                             BigDecimal changePct) {

        // 동일 stock_id + start_date 중복 방지
        if (eventRepository.existsByStockIdAndStartDate(stock.getId(), targetDate)) {
            log.debug("[EventDetection] 이미 존재하는 이벤트 — ticker={}, date={}", stock.getTicker(), targetDate);
            return;
        }

        Event event = Event.create(stock, eventType, targetDate, targetDate,
                changePct, priceBefore, priceAfter);
        eventRepository.save(event);

        log.info("[EventDetection] 이벤트 저장 — ticker={}, type={}, date={}, changePct={}%",
                stock.getTicker(), eventType, targetDate, changePct);
    }
}
