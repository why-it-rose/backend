package com.whyitrose.core.response;

import org.springframework.http.HttpStatus;

public interface ResponseStatus {

    boolean isSuccess();

    HttpStatus getHttpStatus();

    int getResponseCode();

    String getResponseMessage();
}