package com.whyitrose.apiserver.memo.exception;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.whyitrose.core.response.ResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MemoErrorCode implements ResponseStatus {

    MEMO_NOT_FOUND(false, HttpStatus.NOT_FOUND, 4010, "존재하지 않는 메모입니다."),
    EVENT_NOT_FOUND(false, HttpStatus.NOT_FOUND, 4011, "존재하지 않는 이벤트입니다."),
    MEMO_FORBIDDEN(false, HttpStatus.FORBIDDEN, 4012, "메모에 대한 권한이 없습니다.");

    private final boolean isSuccess;

    @JsonIgnore
    private final HttpStatus httpStatus;

    private final int responseCode;
    private final String responseMessage;
}
