package com.whyitrose.batch.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventDetectionScheduler {

    private final EventDetectionService eventDetectionService;

    /**
     * 매주 토요일 오전 10시 실행 — 지난 한 주(월~금) 주가 기준 이벤트 탐지
     * 토요일 기준 직전 월요일(5일 전) ~ 금요일(1일 전)
     */
    @Scheduled(cron = "0 0 10 * * SAT", zone = "Asia/Seoul")
    public void runWeeklyDetection() {
        // 토요일 기준 월(-5일) ~ 금(-1일) 목록 생성
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
