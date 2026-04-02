package com.whyitrose.apiserver.stock.exception;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.whyitrose.core.response.ResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum StockErrorCode implements ResponseStatus {
    STOCK_001(false, HttpStatus.NOT_FOUND, 4101, "존재하지 않는 종목입니다."),
    STOCK_002(false, HttpStatus.NOT_FOUND, 4102, "해당 기간의 차트 데이터가 없습니다."),
    STOCK_003(false, HttpStatus.BAD_GATEWAY, 4103, "외부 시세 API 호출에 실패했습니다."),
    STOCK_004(false, HttpStatus.BAD_REQUEST, 4104, "검색어는 1자 이상이어야 합니다.");

    private final boolean isSuccess;
    @JsonIgnore
    private final HttpStatus httpStatus;
    private final int responseCode;
    private final String responseMessage;
}
