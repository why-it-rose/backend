package com.whyitrose.batch.event;

import com.whyitrose.domain.event.Event;
import com.whyitrose.domain.event.EventRepository;
import com.whyitrose.domain.event.EventType;
import com.whyitrose.domain.stock.Stock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

/**
 * 이벤트 저장 서비스
 *
 * <p>탐지된 이벤트를 DB에 저장한다. 연속 이벤트 병합과 중복 방지 처리를 담당한다.
 *
 * <h3>저장 전략</h3>
 * <ol>
 *   <li><b>병합</b>: 직전 거래일에 동일 종목·동일 방향의 PENDING 이벤트가 있으면 기간을 확장한다.
 *       병합 시 누적 변동률은 최초 시작일 전일 종가 → 당일 종가 기준으로 재계산한다.</li>
 *   <li><b>신규 생성</b>: 병합 대상이 없으면 새 이벤트를 저장한다.</li>
 *   <li><b>중복 방지</b>: 동일 stock_id + start_date 이벤트가 이미 존재하면 저장하지 않는다.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventSaveService {

    private final EventRepository eventRepository;

    /**
     * 이벤트를 저장한다. 병합 가능한 이벤트가 있으면 병합, 없으면 신규 생성한다.
     *
     * @param stock           대상 종목
     * @param eventType       이벤트 타입 (SURGE / DROP)
     * @param targetDate      탐지 날짜 (이벤트 endDate로 사용)
     * @param prevTradingDate 직전 거래일 — 병합 대상 조회 키, 호출자가 이미 조회한 값을 전달받아 중복 쿼리 방지
     * @param priceBefore     시작일 전일 종가 스냅샷 (신규 생성 시에만 사용)
     * @param priceAfter      당일 종가 스냅샷
     * @param changePct       당일 변동률
     * @return 저장(신규 또는 병합) 성공이면 true, 중복으로 skip이면 false
     */
    @Transactional
    public boolean saveEvent(Stock stock, EventType eventType,
                             LocalDate targetDate, LocalDate prevTradingDate,
                             int priceBefore, int priceAfter,
                             BigDecimal changePct) {

        // 직전 거래일에 동일 종목·동일 방향·PENDING 상태이고 거래일 수가 3일 미만인 이벤트를 찾는다.
        // tradingDaysCount < 3 제한: 연속 이벤트를 최대 3거래일까지만 하나로 묶기 위함
        Optional<Event> mergeable = Optional.ofNullable(prevTradingDate).flatMap(prevDate ->
                eventRepository.findMergeable(stock.getId(), eventType, prevDate));

        if (mergeable.isPresent()) {
            // 병합 처리:
            // - endDate를 당일로 확장
            // - priceAfter를 당일 종가로 갱신
            // - changePct는 최초 startDate 전일 종가(priceBefore) 기준으로 재계산 (단순 합산 X)
            Event event = mergeable.get();
            BigDecimal mergedChangePct = calculateChangePct(event.getPriceBefore(), priceAfter);
            event.extend(targetDate, priceAfter, mergedChangePct);

            log.info("[EventDetection] 이벤트 병합 — ticker={}, type={}, startDate={}, endDate={}, changePct={}%",
                    stock.getTicker(), eventType, event.getStartDate(), targetDate, mergedChangePct);
            return true;
        }

        // 재실행 등으로 동일 stock_id + start_date 이벤트가 이미 존재하는 경우 저장하지 않는다.
        if (eventRepository.existsByStockIdAndStartDate(stock.getId(), targetDate)) {
            log.debug("[EventDetection] 이미 존재하는 이벤트 — ticker={}, date={}", stock.getTicker(), targetDate);
            return false;
        }

        // 신규 이벤트 생성 — status, crawlStatus 모두 PENDING으로 초기화
        // PENDING → AI 요약 생성 후 ACTIVE로 전환되는 흐름
        Event event = Event.create(stock, eventType, targetDate, targetDate,
                changePct, priceBefore, priceAfter);
        eventRepository.save(event);

        log.info("[EventDetection] 이벤트 저장 — ticker={}, type={}, date={}, changePct={}%",
                stock.getTicker(), eventType, targetDate, changePct);
        return true;
    }

    /**
     * 누적 변동률 재계산 — 병합 시 사용
     *
     * <p>병합 이벤트의 changePct는 최초 startDate 전일 종가(priceBefore)와
     * 현재 당일 종가(priceAfter)의 실제 등락률로 재계산한다.
     * 일별 변동률을 단순 합산하면 복리 효과가 누락되므로 이 방식을 사용한다.
     */
    private BigDecimal calculateChangePct(int priceBefore, int priceAfter) {
        return BigDecimal.valueOf(priceAfter - priceBefore)
                .divide(BigDecimal.valueOf(priceBefore), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
