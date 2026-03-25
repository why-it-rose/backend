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
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSaveService {

    private final EventRepository eventRepository;

    /**
     * 이벤트 저장 (중복 방지 포함)
     * - 별도 클래스로 분리하여 @Transactional self-invocation 문제 해결
     *
     * @return 저장 성공 여부
     */
    @Transactional
    public boolean saveEvent(Stock stock, EventType eventType,
                             LocalDate targetDate, int priceBefore, int priceAfter,
                             BigDecimal changePct) {

        // 동일 stock_id + start_date 중복 방지
        if (eventRepository.existsByStockIdAndStartDate(stock.getId(), targetDate)) {
            log.debug("[EventDetection] 이미 존재하는 이벤트 — ticker={}, date={}", stock.getTicker(), targetDate);
            return false;
        }

        Event event = Event.create(stock, eventType, targetDate, targetDate,
                changePct, priceBefore, priceAfter);
        eventRepository.save(event);

        log.info("[EventDetection] 이벤트 저장 — ticker={}, type={}, date={}, changePct={}%",
                stock.getTicker(), eventType, targetDate, changePct);
        return true;
    }
}
