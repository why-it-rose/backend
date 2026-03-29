package com.whyitrose.apiserver.memo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "메모 수정 요청")
public record MemoUpdateRequest(

        @Schema(description = "수정할 메모 내용 (최대 300자)", example = "단기 과열은 있었지만 수급이 생각보다 오래 유지됨.")
        @NotBlank(message = "메모 내용을 입력해주세요.")
        @Size(max = 300, message = "메모는 최대 300자까지 입력 가능합니다.")
        String content
) {
}
