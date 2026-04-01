package com.whyitrose.batch.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 이벤트 탐지 수동 재실행 Runner
 *
 * <p>스케줄러 장애 등으로 특정 기간의 이벤트 탐지가 누락됐을 때 수동으로 재실행한다.
 * 배치 앱은 {@code web-application-type: none}이라 REST API를 제공할 수 없으므로,
 * 앱 시작 시 커맨드라인 인수로 날짜 범위를 받아 실행한다.
 *
 * <h3>실행 방법</h3>
 * <pre>
 * # 단일 날짜 재실행
 * java -jar batch.jar --event-detection.from=2024-01-15 --event.detection.runner.enabled=true
 *
 * # 날짜 범위 재실행
 * java -jar batch.jar --event-detection.from=2024-01-13 --event-detection.to=2024-01-19 --event.detection.runner.enabled=true
 * </pre>
 *
 * <h3>활성화 조건</h3>
 * {@code event.detection.runner.enabled=true}일 때만 동작한다. (기본값 비활성)
 * 스케줄러와 동시 활성화하면 의도치 않은 중복 실행이 발생할 수 있으므로 주의한다.
 *
 * <h3>주말 처리</h3>
 * 토·일요일은 주가 데이터가 없어 skip한다.
 * 공휴일은 skip하지 않지만, 주가 데이터가 없으면 내부적으로 skip된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "event.detection.runner.enabled", havingValue = "true")
public class EventDetectionRunner implements ApplicationRunner {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final EventDetectionService eventDetectionService;

    @Override
    public void run(ApplicationArguments args) {
        LocalDate from = parseDate(args, "event-detection.from");
        if (from == null) {
            log.warn("[EventDetectionRunner] --event-detection.from 인수가 없습니다. 실행 중단.");
            return;
        }

        // to 생략 시 from 하루만 실행
        LocalDate to = parseDate(args, "event-detection.to");
        if (to == null) {
            to = from;
        }

        if (to.isBefore(from)) {
            log.warn("[EventDetectionRunner] to({})가 from({})보다 이전입니다. 실행 중단.", to, from);
            return;
        }

        log.info("[EventDetectionRunner] 수동 이벤트 탐지 시작 — {} ~ {}", from, to);

        int totalCreated = 0;
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            DayOfWeek dow = cursor.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                // 주말은 한국 주식 시장 휴장 — 주가 데이터가 없으므로 불필요한 DB 조회를 방지
                log.debug("[EventDetectionRunner] 주말 skip — {}", cursor);
                cursor = cursor.plusDays(1);
                continue;
            }
            try {
                int created = eventDetectionService.detectAndSaveEvents(cursor);
                totalCreated += created;
            } catch (Exception e) {
                log.error("[EventDetectionRunner] 탐지 실패 — targetDate={}, msg={}", cursor, e.getMessage(), e);
            }
            cursor = cursor.plusDays(1);
        }

        log.info("[EventDetectionRunner] 수동 이벤트 탐지 완료 — {} ~ {}, 총 생성 이벤트: {}개", from, to, totalCreated);
    }

    /**
     * 커맨드라인 인수에서 날짜 값을 파싱한다.
     *
     * @param key 인수 키 (예: "event-detection.from")
     * @return 파싱 성공 시 LocalDate, 인수 없거나 형식 오류 시 null
     */
    private LocalDate parseDate(ApplicationArguments args, String key) {
        List<String> values = args.getOptionValues(key);
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(values.get(0), DATE_FMT);
        } catch (DateTimeParseException e) {
            log.warn("[EventDetectionRunner] {} 날짜 파싱 실패: {}", key, values.get(0));
            return null;
        }
    }
}
