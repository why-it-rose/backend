package com.whyitrose.apiserver.global.exception;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.whyitrose.core.response.ResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CommonErrorCode implements ResponseStatus {
    COMMON_001(false, HttpStatus.INTERNAL_SERVER_ERROR, 5001, "서버 내부 오류가 발생했습니다.");

    private final boolean isSuccess;
    @JsonIgnore
    private final HttpStatus httpStatus;
    private final int responseCode;
    private final String responseMessage;
}
