package com.whyitrose.apiserver.scrap.exception;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.whyitrose.core.response.ResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ScrapErrorCode implements ResponseStatus {

    SCRAP_NOT_FOUND(false, HttpStatus.NOT_FOUND, 4020, "존재하지 않는 스크랩입니다."),
    EVENT_NOT_FOUND(false, HttpStatus.NOT_FOUND, 4021, "존재하지 않는 이벤트입니다."),
    ALREADY_SCRAPED(false, HttpStatus.CONFLICT, 4022, "이미 스크랩한 이벤트입니다.");

    private final boolean isSuccess;
    @JsonIgnore
    private final HttpStatus httpStatus;
    private final int responseCode;
    private final String responseMessage;
}
