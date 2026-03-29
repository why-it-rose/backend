package com.whyitrose.apiserver.memo.dto;

import com.whyitrose.domain.memo.Memo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "메모 응답")
public record MemoResponse(

        @Schema(description = "메모 ID", example = "1")
        Long memoId,

        @Schema(description = "메모 내용", example = "HBM 납품 기대감으로 외국인 매수세가 강하게 불은 구간.")
        String content,

        @Schema(description = "작성 일시", example = "2026-03-16T10:30:00")
        LocalDateTime createdAt
) {
    public static MemoResponse from(Memo memo) {
        return new MemoResponse(
                memo.getId(),
                memo.getContent(),
                memo.getCreatedAt()
        );
    }
}
