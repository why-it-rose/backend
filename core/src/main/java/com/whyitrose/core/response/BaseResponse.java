package com.whyitrose.core.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

import static com.whyitrose.core.response.BaseResponseStatus.SUCCESS;

@Getter
@JsonPropertyOrder({"isSuccess", "responseCode", "responseMessage", "result"})
public class BaseResponse<T> {

    @JsonProperty("isSuccess")
    private final Boolean isSuccess;

    private final int responseCode;
    private final String responseMessage;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final T result;

    private BaseResponse(T result) {
        this.isSuccess = SUCCESS.isSuccess();
        this.responseCode = SUCCESS.getResponseCode();
        this.responseMessage = SUCCESS.getResponseMessage();
        this.result = result;
    }

    private BaseResponse(ResponseStatus status) {
        this.isSuccess = status.isSuccess();
        this.responseCode = status.getResponseCode();
        this.responseMessage = status.getResponseMessage();
        this.result = null;
    }

    private BaseResponse(ResponseStatus status, T result) {
        this.isSuccess = status.isSuccess();
        this.responseCode = status.getResponseCode();
        this.responseMessage = status.getResponseMessage();
        this.result = result;
    }

    public static <T> BaseResponse<T> success(T result) {
        return new BaseResponse<>(result);
    }

    public static <T> BaseResponse<T> failure(ResponseStatus status) {
        return new BaseResponse<>(status);
    }

    public static <T> BaseResponse<T> failure(ResponseStatus status, T result) {
        return new BaseResponse<>(status, result);
    }
}