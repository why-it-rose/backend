package com.whyitrose.batch.event;

import com.whyitrose.domain.common.Status;
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
import java.util.OptionalDouble;

/**
 * 이벤트 탐지 서비스
 *
 * <p>ACTIVE 종목 전체를 대상으로 특정 날짜의 급등/급락 이벤트를 탐지한다.
 * 탐지 조건을 모두 충족한 종목은 {@link EventSaveService}를 통해 이벤트로 저장된다.
 *
 * <h3>탐지 조건 (AND)</h3>
 * <ul>
 *   <li>가격 조건: 전일 종가 대비 변동률이 +5% 이상(급등) 또는 -5% 이하(급락)</li>
 *   <li>거래량 조건: 당일 거래량이 직전 20 거래일 평균 거래량의 1.5배 이상</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventDetectionService {

    /** 급등 판단 기준: 전일 대비 +5.00% 이상 */
    private static final BigDecimal SURGE_THRESHOLD = new BigDecimal("5.00");

    /** 급락 판단 기준: 전일 대비 -5.00% 이하 */
    private static final BigDecimal DROP_THRESHOLD  = new BigDecimal("-5.00");

    /** 거래량 평균 계산에 사용할 직전 거래일 수 */
    private static final int    VOLUME_LOOKBACK_DAYS   = 20;

    /** 평균 거래량 대비 당일 거래량 배율 기준 (1.5배 이상이어야 이벤트로 인정) */
    private static final double VOLUME_RATIO_THRESHOLD = 1.5;

    private final StockRepository      stockRepository;
    private final StockPriceRepository stockPriceRepository;
    private final EventSaveService     eventSaveService;

    /**
     * 지정 날짜 하루치에 대해 전체 ACTIVE 종목의 이벤트를 탐지하고 저장한다.
     *
     * <p>종목별 처리 중 예외가 발생해도 다른 종목 탐지는 계속 진행된다.
     *
     * @param targetDate 탐지 대상 날짜
     * @return 새로 생성된 이벤트 수 (병합 포함)
     */
    @Transactional
    public int detectAndSaveEvents(LocalDate targetDate) {
        List<Stock> activeStocks = stockRepository.findByStatus(Status.ACTIVE);
        log.info("[EventDetection] 탐지 시작 — targetDate={}, 대상종목={}개", targetDate, activeStocks.size());

        int createdCount = 0;
        for (Stock stock : activeStocks) {
            try {
                if (detectForStock(stock, targetDate)) {
                    createdCount++;
                }
            } catch (Exception e) {
                // 한 종목의 오류가 전체 탐지를 중단시키지 않도록 개별 catch
                log.warn("[EventDetection] 종목 처리 중 오류 — ticker={}, date={}, msg={}",
                        stock.getTicker(), targetDate, e.getMessage());
            }
        }

        log.info("[EventDetection] 탐지 완료 — targetDate={}, 생성={}개", targetDate, createdCount);
        return createdCount;
    }

    // ── 종목별 단일일 이벤트 탐지 ─────────────────────────────────────

    /**
     * 단일 종목에 대해 특정 날짜의 이벤트 탐지를 수행한다.
     *
     * <p>직전 20 거래일치를 단일 쿼리로 조회해 전일 비교와 거래량 평균 계산에 함께 활용한다.
     *
     * @return 이벤트가 저장(신규 또는 병합)되면 true, 조건 미충족·skip이면 false
     */
    private boolean detectForStock(Stock stock, LocalDate targetDate) {
        StockPrice curr = stockPriceRepository
                .findByStockIdAndTradingDate(stock.getId(), targetDate)
                .orElse(null);

        if (curr == null) {
            return false; // 해당일 주가 데이터 없음 (휴장일·데이터 미수집 등)
        }

        // 직전 최대 20 거래일치를 한 번에 조회한다.
        // - recentPrices.get(0): 가장 최근 거래일 → 전일 종가 비교에 사용
        // - recentPrices 전체: 20일 평균 거래량 계산에 사용
        // prev 조회(1건)와 거래량 조회(20건)를 단일 쿼리로 처리해 DB 왕복을 줄인다.
        List<StockPrice> recentPrices = stockPriceRepository.findRecentPricesBeforeDate(
                stock.getId(), targetDate, PageRequest.of(0, VOLUME_LOOKBACK_DAYS));

        if (recentPrices.isEmpty()) {
            return false; // 비교할 전일 데이터 없음 (신규 상장 등)
        }

        StockPrice prev = recentPrices.get(0); // 직전 거래일 = 리스트의 첫 번째 항목

        // ① 변동률 계산 — 소수점 2자리 반올림
        BigDecimal changePct = calculateChangePct(prev.getClosePrice(), curr.getClosePrice());

        // ② 가격 조건 판단 — ±5% 기준 미충족 시 조기 종료
        EventType eventType = resolveEventType(changePct);
        if (eventType == null) {
            return false;
        }

        // ③ 거래량 조건 판단 — 이미 조회한 recentPrices를 재사용해 추가 쿼리 없이 처리
        if (!isVolumeConditionMet(recentPrices, curr.getVolume())) {
            log.debug("[EventDetection] 거래량 조건 미충족 — ticker={}, date={}", stock.getTicker(), targetDate);
            return false;
        }

        // ④ 이벤트 저장 — 이미 조회한 prev.getTradingDate()를 전달해 EventSaveService의 중복 쿼리 방지
        return eventSaveService.saveEvent(
                stock, eventType, targetDate,
                prev.getTradingDate(), prev.getClosePrice(), curr.getClosePrice(), changePct);
    }

    // ── 변동률 계산 ───────────────────────────────────────────────────

    /**
     * 전일 종가 대비 당일 종가 변동률을 계산한다.
     *
     * <pre>(priceAfter - priceBefore) / priceBefore × 100</pre>
     *
     * 중간 연산은 소수점 6자리로 유지하다가 최종 결과를 소수점 2자리로 반올림한다.
     */
    private BigDecimal calculateChangePct(int priceBefore, int priceAfter) {
        return BigDecimal.valueOf(priceAfter - priceBefore)
                .divide(BigDecimal.valueOf(priceBefore), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    // ── 이벤트 타입 결정 ──────────────────────────────────────────────

    /**
     * 변동률로 이벤트 타입을 결정한다.
     *
     * @return 급등이면 SURGE, 급락이면 DROP, 임계값 미달이면 null
     */
    private EventType resolveEventType(BigDecimal changePct) {
        if (changePct.compareTo(SURGE_THRESHOLD) >= 0) return EventType.SURGE;
        if (changePct.compareTo(DROP_THRESHOLD)  <= 0) return EventType.DROP;
        return null;
    }

    // ── 거래량 조건 검증 ──────────────────────────────────────────────

    /**
     * 당일 거래량이 직전 20 거래일 평균의 1.5배 이상인지 검증한다.
     *
     * <p>평균 거래량이 0이거나 계산 불가 시 조건 미충족으로 처리한다.
     *
     * @param recentPrices 직전 거래일 목록 (거래량 평균 계산 대상, detectForStock에서 조회한 것을 재사용)
     * @param currentVolume 당일 거래량
     */
    private boolean isVolumeConditionMet(List<StockPrice> recentPrices, long currentVolume) {
        OptionalDouble avgVolumeOpt = recentPrices.stream()
                .mapToLong(StockPrice::getVolume)
                .average();

        if (avgVolumeOpt.isEmpty() || avgVolumeOpt.getAsDouble() == 0) {
            return false;
        }

        return currentVolume >= avgVolumeOpt.getAsDouble() * VOLUME_RATIO_THRESHOLD;
    }
}
