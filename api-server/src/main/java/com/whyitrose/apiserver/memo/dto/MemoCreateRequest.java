package com.whyitrose.apiserver.memo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "메모 작성 요청")
public record MemoCreateRequest(

        @Schema(description = "메모 내용 (최대 300자)", example = "HBM 납품 기대감으로 외국인 매수세가 강하게 불은 구간.")
        @NotBlank(message = "메모 내용을 입력해주세요.")
        @Size(max = 300, message = "메모는 최대 300자까지 입력 가능합니다.")
        String content
) {
}
