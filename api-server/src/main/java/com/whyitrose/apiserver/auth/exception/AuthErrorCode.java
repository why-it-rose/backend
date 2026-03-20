package com.whyitrose.apiserver.auth.exception;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.whyitrose.core.response.ResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements ResponseStatus {
    AUTH_002(false, HttpStatus.BAD_REQUEST, 4002, "이미 가입된 이메일입니다."),
    AUTH_009(false, HttpStatus.BAD_REQUEST, 4009, "입력값이 유효하지 않습니다."),
    AUTH_010(false, HttpStatus.BAD_REQUEST, 4010, "이미 사용 중인 닉네임입니다.");

    private final boolean isSuccess;
    @JsonIgnore
    private final HttpStatus httpStatus;
    private final int responseCode;
    private final String responseMessage;
}
