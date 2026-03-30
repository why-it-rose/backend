package com.whyitrose.apiserver.event.dto;

import com.whyitrose.domain.event.EventNews;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "이벤트 연관 뉴스")
public record EventNewsResponse(
        @Schema(description = "뉴스 ID", example = "42")
        Long newsId,

        @Schema(description = "뉴스 제목", example = "삼성전자, 반도체 수요 급증으로 주가 급등")
        String title,

        @Schema(description = "언론사", example = "한국경제")
        String source,

        @Schema(description = "뉴스 원문 URL", example = "https://www.hankyung.com/article/123456")
        String url,

        @Schema(description = "썸네일 이미지 URL", example = "https://img.hankyung.com/photo/123456.jpg")
        String thumbnailUrl,

        @Schema(description = "기사 발행 시각", example = "2024-03-16T09:30:00")
        LocalDateTime publishedAt,

        @Schema(description = "이벤트 관련도 점수 (0~1)", example = "0.9231")
        BigDecimal relevanceScore,

        @Schema(description = "뉴스 태그(최대 2개)", example = "[\"반도체\", \"실적\"]")
        List<String> tags
) {
    public static EventNewsResponse from(EventNews eventNews, List<String> tags) {
        return new EventNewsResponse(
                eventNews.getNews().getId(),
                eventNews.getNews().getTitle(),
                eventNews.getNews().getSource(),
                eventNews.getNews().getUrl(),
                eventNews.getNews().getThumbnailUrl(),
                eventNews.getNews().getPublishedAt(),
                eventNews.getRelevanceScore(),
                tags
        );
    }
}
