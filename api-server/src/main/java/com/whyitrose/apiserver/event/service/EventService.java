package com.whyitrose.apiserver.event.service;

import com.whyitrose.apiserver.event.dto.EventDetailResponse;
import com.whyitrose.apiserver.event.dto.EventResponse;
import com.whyitrose.apiserver.event.dto.StockLatestEventResponse;
import com.whyitrose.apiserver.event.exception.EventErrorCode;
import com.whyitrose.core.exception.BaseException;
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
import java.util.Optional;
import java.util.OptionalDouble;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private static final BigDecimal SURGE_THRESHOLD      = new BigDecimal("5.00");
    private static final BigDecimal DROP_THRESHOLD       = new BigDecimal("-5.00");
    private static final BigDecimal MAX_CHANGE_THRESHOLD = new BigDecimal("30.00"); // 권리락/액면분할 필터
    private static final int        VOLUME_LOOKBACK_DAYS = 20;
    private static final double     VOLUME_RATIO         = 1.5;

    private final StockRepository      stockRepository;
    private final StockPriceRepository stockPriceRepository;
    private final EventRepository      eventRepository;

    // ── 이벤트 목록 조회 ─────────────────────────────────────────────────

    public List<EventResponse> getEvents(Long stockId, EventType eventType, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        List<Event> events = (eventType != null)
                ? eventRepository.findByStockIdAndStatusAndEventTypeOrderByStartDateDesc(stockId, Status.ACTIVE, eventType, pageable)
                : eventRepository.findByStockIdAndStatusOrderByStartDateDesc(stockId, Status.ACTIVE, pageable);

        return events.stream()
                .map(EventResponse::from)
                .toList();
    }

    // ── 종목 목록의 최신 이벤트 조회 ──────────────────────────────────────

    public List<StockLatestEventResponse> getLatestEventsByStockIds(List<Long> stockIds) {
        return eventRepository.findLatestByStockIds(stockIds, Status.ACTIVE)
                .stream()
                .map(StockLatestEventResponse::from)
                .toList();
    }

    // ── 이벤트 상세 조회 ─────────────────────────────────────────────────

    public EventDetailResponse getEventDetail(Long eventId) {
        Event event = eventRepository.findByIdWithNews(eventId)
                .orElseThrow(() -> new BaseException(EventErrorCode.EVENT_NOT_FOUND));

        return EventDetailResponse.from(event);
    }

    // ── 이벤트 탐지 실행 ─────────────────────────────────────────────────

    @Transactional
    public int detectEventsForRange(LocalDate from, LocalDate to) {
        int totalCreated = 0;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            List<Stock> activeStocks = stockRepository.findByStatus(Status.ACTIVE);
            for (Stock stock : activeStocks) {
                try {
                    if (detectForStock(stock, date)) {
                        totalCreated++;
                    }
                } catch (Exception e) {
                    log.warn("[EventDetect] 처리 오류 — ticker={}, date={}, msg={}",
                            stock.getTicker(), date, e.getMessage());
                }
            }
        }
        log.info("[EventDetect] 기간 탐지 완료 — {} ~ {}, 총 생성/병합={}건", from, to, totalCreated);
        return totalCreated;
    }

    @Transactional
    public void detectEvents(LocalDate targetDate) {
        List<Stock> activeStocks = stockRepository.findByStatus(Status.ACTIVE);
        log.info("[EventDetect] 탐지 시작 — date={}, 종목수={}개", targetDate, activeStocks.size());

        for (Stock stock : activeStocks) {
            try {
                detectForStock(stock, targetDate);
            } catch (Exception e) {
                log.warn("[EventDetect] 처리 오류 — ticker={}, msg={}", stock.getTicker(), e.getMessage());
            }
        }

        log.info("[EventDetect] 탐지 완료 — date={}", targetDate);
    }

    // ── 종목별 탐지 ─────────────────────────────────────────────────────

    private boolean detectForStock(Stock stock, LocalDate targetDate) {
        StockPrice curr = stockPriceRepository
                .findByStockIdAndTradingDate(stock.getId(), targetDate)
                .orElse(null);

        if (curr == null) return false;

        List<StockPrice> prevList = stockPriceRepository
                .findRecentPricesBeforeDate(stock.getId(), targetDate, PageRequest.of(0, 1));

        if (prevList.isEmpty()) return false;

        StockPrice prev = prevList.get(0);
        BigDecimal changePct = calculateChangePct(prev.getClosePrice(), curr.getClosePrice());

        // 권리락/액면분할 필터 — ±30% 초과는 이벤트로 보지 않음
        if (changePct.abs().compareTo(MAX_CHANGE_THRESHOLD) > 0) {
            log.debug("[EventDetect] 변동률 초과 (권리락/분할 의심) — ticker={}, date={}, changePct={}%",
                    stock.getTicker(), targetDate, changePct);
            return false;
        }

        EventType eventType = resolveEventType(changePct);
        if (eventType == null) return false;

        if (!isVolumeConditionMet(stock.getId(), targetDate, curr.getVolume())) {
            log.debug("[EventDetect] 거래량 미충족 — ticker={}, date={}", stock.getTicker(), targetDate);
            return false;
        }

        return saveOrMergeEvent(stock, eventType, targetDate, prev.getClosePrice(), curr.getClosePrice(), changePct);
    }

    // ── 이벤트 저장 또는 병합 ────────────────────────────────────────────

    private boolean saveOrMergeEvent(Stock stock, EventType eventType,
                                     LocalDate targetDate, int priceBefore, int priceAfter,
                                     BigDecimal changePct) {
        // 직전 거래일 조회
        Optional<LocalDate> prevTradingDate = stockPriceRepository
                .findRecentPricesBeforeDate(stock.getId(), targetDate, PageRequest.of(0, 1))
                .stream()
                .map(StockPrice::getTradingDate)
                .findFirst();

        // 병합 대상 이벤트 조회
        Optional<Event> mergeable = prevTradingDate.flatMap(prevDate ->
                eventRepository.findMergeable(stock.getId(), eventType, prevDate));

        if (mergeable.isPresent()) {
            Event event = mergeable.get();
            BigDecimal mergedChangePct = calculateChangePct(event.getPriceBefore(), priceAfter);
            event.extend(targetDate, priceAfter, mergedChangePct);

            log.info("[EventDetect] 이벤트 병합 — ticker={}, type={}, startDate={}, endDate={}, changePct={}%",
                    stock.getTicker(), eventType, event.getStartDate(), targetDate, mergedChangePct);
            return true;
        }

        // 중복 방지
        if (eventRepository.existsByStockIdAndStartDate(stock.getId(), targetDate)) {
            log.debug("[EventDetect] 중복 이벤트 — ticker={}, date={}", stock.getTicker(), targetDate);
            return false;
        }

        // 신규 이벤트 생성
        Event event = Event.create(stock, eventType, targetDate, targetDate,
                changePct, priceBefore, priceAfter);
        eventRepository.save(event);

        log.info("[EventDetect] 이벤트 저장 — ticker={}, type={}, date={}, changePct={}%",
                stock.getTicker(), eventType, targetDate, changePct);
        return true;
    }

    // ── 변동률 계산 ──────────────────────────────────────────────────────

    private BigDecimal calculateChangePct(int priceBefore, int priceAfter) {
        return BigDecimal.valueOf(priceAfter - priceBefore)
                .divide(BigDecimal.valueOf(priceBefore), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    // ── 이벤트 타입 결정 ─────────────────────────────────────────────────

    private EventType resolveEventType(BigDecimal changePct) {
        if (changePct.compareTo(SURGE_THRESHOLD) >= 0) return EventType.SURGE;
        if (changePct.compareTo(DROP_THRESHOLD)  <= 0) return EventType.DROP;
        return null;
    }

    // ── 거래량 조건 검증 ─────────────────────────────────────────────────

    private boolean isVolumeConditionMet(Long stockId, LocalDate targetDate, long currentVolume) {
        List<StockPrice> recentPrices = stockPriceRepository
                .findRecentPricesBeforeDate(stockId, targetDate, PageRequest.of(0, VOLUME_LOOKBACK_DAYS));

        if (recentPrices.isEmpty()) return false;

        // 거래정지일(volume = 0) 제외 후 평균 계산
        OptionalDouble avgVolumeOpt = recentPrices.stream()
                .filter(sp -> sp.getVolume() > 0)
                .mapToLong(StockPrice::getVolume)
                .average();

        if (avgVolumeOpt.isEmpty() || avgVolumeOpt.getAsDouble() == 0) {
            log.debug("[EventDetect] 거래량 평균 계산 불가 — stockId={}, date={}", stockId, targetDate);
            return false;
        }

        return currentVolume >= avgVolumeOpt.getAsDouble() * VOLUME_RATIO;
    }
}
