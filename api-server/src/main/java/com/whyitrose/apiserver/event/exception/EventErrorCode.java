package com.whyitrose.apiserver.event.exception;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.whyitrose.core.response.ResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum EventErrorCode implements ResponseStatus {

    EVENT_NOT_FOUND(false, HttpStatus.NOT_FOUND, 4001, "존재하지 않는 이벤트입니다."),
    STOCK_NOT_FOUND(false, HttpStatus.NOT_FOUND, 4002, "존재하지 않는 종목코드입니다."),
    INVALID_DETECT_DATE(false, HttpStatus.BAD_REQUEST, 4003, "탐지 날짜를 입력해주세요.");

    private final boolean isSuccess;

    @JsonIgnore
    private final HttpStatus httpStatus;

    private final int responseCode;
    private final String responseMessage;
}
