package com.whyitrose.apiserver.auth.exception;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.whyitrose.core.response.ResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements ResponseStatus {
    //auth 오류 3가지
    AUTH_002(false, HttpStatus.BAD_REQUEST, 4002, "이미 가입된 이메일입니다."),
    AUTH_009(false, HttpStatus.BAD_REQUEST, 4009, "입력값이 유효하지 않습니다."),
    AUTH_010(false, HttpStatus.BAD_REQUEST, 4010, "이미 사용 중인 닉네임입니다."),
    AUTH_011(false, HttpStatus.BAD_REQUEST, 4011, "이메일 또는 비밀번호가 올바르지 않습니다."),
    AUTH_012(false, HttpStatus.BAD_REQUEST, 4012, "소셜 가입 계정은 이메일 로그인이 불가합니다."),
    AUTH_013(false, HttpStatus.BAD_REQUEST, 4013, "탈퇴한 계정입니다.");


    private final boolean isSuccess;
    @JsonIgnore
    private final HttpStatus httpStatus;
    private final int responseCode;
    private final String responseMessage;
}
