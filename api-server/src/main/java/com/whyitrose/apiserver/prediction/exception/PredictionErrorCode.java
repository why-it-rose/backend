package com.whyitrose.apiserver.prediction.exception;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.whyitrose.core.response.ResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PredictionErrorCode implements ResponseStatus {

    DIGEST_NOT_FOUND(false, HttpStatus.NOT_FOUND, 4030, "존재하지 않는 학습 다이제스트입니다."),
    PREDICTION_DATE_INVALID(false, HttpStatus.BAD_REQUEST, 4031, "예측은 오늘의 학습에서만 가능합니다."),
    STOCK_NOT_FOUND(false, HttpStatus.NOT_FOUND, 4032, "존재하지 않는 종목입니다."),
    USER_NOT_FOUND(false, HttpStatus.NOT_FOUND, 4033, "존재하지 않는 사용자입니다.");

    private final boolean isSuccess;
    @JsonIgnore
    private final HttpStatus httpStatus;
    private final int responseCode;
    private final String responseMessage;
}