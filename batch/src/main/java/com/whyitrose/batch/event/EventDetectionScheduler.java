package com.whyitrose.batch.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 이벤트 탐지 스케줄러
 *
 * <p>매주 토요일 오전 10시에 직전 한 주(월~금) 전체에 대해 이벤트 탐지를 실행한다.
 * 주중에 실행하지 않고 주말에 일괄 처리하는 이유:
 * <ul>
 *   <li>주가 데이터가 당일 장 마감 후에 수집되므로, 주중 실시간 탐지보다 주 단위 배치가 안정적</li>
 *   <li>탐지 결과(이벤트)는 AI 요약 생성 후 ACTIVE 전환되므로 실시간성이 필수적이지 않음</li>
 * </ul>
 *
 * <h3>활성화 조건</h3>
 * {@code event.detection.scheduler.enabled=true} 프로퍼티가 설정된 환경에서만 동작한다.
 * (로컬/개발 환경 분리 목적)
 *
 * <h3>수동 재실행</h3>
 * 장애 등으로 탐지를 놓쳤을 경우 {@link EventDetectionRunner}를 사용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "event.detection.scheduler.enabled", havingValue = "true")
public class EventDetectionScheduler {

    private final EventDetectionService eventDetectionService;

    /**
     * 매주 토요일 오전 10시 실행 (Asia/Seoul 기준)
     *
     * <p>토요일 기준으로 직전 5 거래일(월~금)을 계산해 순서대로 탐지한다.
     * 날짜별로 개별 try-catch하므로, 특정 날짜 탐지 실패가 다른 날짜 처리를 중단시키지 않는다.
     */
    @Scheduled(cron = "0 0 10 * * SAT", zone = "Asia/Seoul")
    public void runWeeklyDetection() {
        // 토요일 기준: -1일=금, -2일=목, -3일=수, -4일=화, -5일=월
        List<LocalDate> weekdays = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> LocalDate.now().minusDays(i))
                .sorted()
                .toList();

        log.info("[Scheduler] 주간 이벤트 탐지 시작 — {} ~ {}", weekdays.get(0), weekdays.get(weekdays.size() - 1));

        int totalCreated = 0;
        for (LocalDate targetDate : weekdays) {
            try {
                int created = eventDetectionService.detectAndSaveEvents(targetDate);
                totalCreated += created;
            } catch (Exception e) {
                log.error("[Scheduler] 탐지 실패 — targetDate={}, msg={}", targetDate, e.getMessage(), e);
            }
        }

        log.info("[Scheduler] 주간 이벤트 탐지 종료 — 기간: {} ~ {}, 총 생성 이벤트: {}개",
                weekdays.get(0), weekdays.get(weekdays.size() - 1), totalCreated);
    }
}
