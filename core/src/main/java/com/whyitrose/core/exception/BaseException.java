package com.whyitrose.core.exception;

import com.whyitrose.core.response.ResponseStatus;
import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {

    private final ResponseStatus status;

    public BaseException(ResponseStatus status) {
        super(status.getResponseMessage());
        this.status = status;
    }
}