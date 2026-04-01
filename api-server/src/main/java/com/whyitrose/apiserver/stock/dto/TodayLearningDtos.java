package com.whyitrose.apiserver.stock.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.whyitrose.domain.news.News;
import com.whyitrose.domain.prediction.Prediction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TodayLearningDtos {

    private static final DateTimeFormatter DATE_DOT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final String[] KOREAN_DAYS = {"월", "화", "수", "목", "금", "토", "일"};

    // ──────────────────────────────────────────────
    // 엔드포인트 1: GET /api/stocks/{stockId}/learning-pin
    // ──────────────────────────────────────────────

    public record LearningPinResponse(
            String digestDate,   // "2026-03-29"
            int newsCount
    ) {
        public static LearningPinResponse of(LocalDate digestDate, int newsCount) {
            return new LearningPinResponse(digestDate.toString(), newsCount);
        }
    }

    // ──────────────────────────────────────────────
    // 엔드포인트 2: GET /api/stocks/{stockId}/today-learning
    // ──────────────────────────────────────────────

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TodayLearningDetailResponse(
            Long digestId,              // 예측 API 호출 시 사용
            String digestDate,          // "2026.03.29 (일)"
            String stockName,
            String changeRate,          // "+1.23%" — 전전날 종가 대비 전날 종가 (없으면 필드 생략)
            Integer priceClose,         // 전날(digestDate) 종가 (없으면 필드 생략)
            Integer prevPriceClose,     // 전전날 종가 (없으면 필드 생략)
            PredictionInfo prediction,  // 예측 완료 시만 포함 (비로그인·미예측 시 필드 생략)
            List<LearningNewsItem> newsList
    ) {}

    /** 예측 완료 시에만 응답에 포함됨 — 모든 필드 non-null */
    public record PredictionInfo(
            Long predictionId,
            String direction,    // "UP" | "DOWN" | "SIDEWAYS"
            String reason
    ) {
        public static PredictionInfo from(Prediction prediction) {
            return new PredictionInfo(
                    prediction.getId(),
                    prediction.getDirection().name(),
                    prediction.getReason()
            );
        }
    }

    public record LearningNewsItem(
            Long newsId,
            String title,
            String summary,       // content 앞 100자
            String source,
            String publishedAt,   // "2026.03.29"
            String url,
            List<String> tags
    ) {
        public static LearningNewsItem from(News news, List<String> tags) {
            String content = news.getContent();
            String summary = content.length() > 100 ? content.substring(0, 100) : content;
            return new LearningNewsItem(
                    news.getId(),
                    news.getTitle(),
                    summary,
                    news.getSource(),
                    news.getPublishedAt().format(DATE_DOT),
                    news.getUrl(),
                    tags
            );
        }
    }

    /** "yyyy.MM.dd (요일)" 형식으로 포맷 */
    public static String formatDigestDate(LocalDate date) {
        String day = KOREAN_DAYS[date.getDayOfWeek().getValue() - 1];
        return date.format(DATE_DOT) + " (" + day + ")";
    }
}