package com.whyitrose.core.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BaseResponseStatus implements ResponseStatus {

    /**
     * 1000: 요청 성공
     */
    SUCCESS(true, HttpStatus.OK, 1000, "요청에 성공하였습니다."),

    /**
     * 2000: Request 예외
     */
    METHOD_ARGUMENT_TYPE_MISMATCH(false, HttpStatus.BAD_REQUEST, 2001, "요청 파라미터 타입이 올바르지 않습니다."),
    HTTP_METHOD_TYPE_MISMATCH(false, HttpStatus.METHOD_NOT_ALLOWED, 2002, "지원되지 않는 HTTP Method입니다."),

    /**
     * 2010: 알림 예외
     */
    NOTIFICATION_NOT_FOUND(false, HttpStatus.NOT_FOUND, 2010, "존재하지 않는 알림입니다."),
    NOTIFICATION_FORBIDDEN(false, HttpStatus.FORBIDDEN, 2011, "접근 권한이 없는 알림입니다."),

    /**
     * 2900: JWT & 인증 예외
     */
    INVALID_TOKEN(false, HttpStatus.UNAUTHORIZED, 2901, "유효하지 않은 JWT 토큰입니다."),
    EXPIRED_TOKEN(false, HttpStatus.UNAUTHORIZED, 2902, "만료된 JWT 토큰입니다."),
    BLACKLISTED_REFRESH_TOKEN(false, HttpStatus.UNAUTHORIZED, 2903, "사용이 중지된 리프레시 토큰입니다."),
    NOT_REFRESH_TOKEN(false, HttpStatus.BAD_REQUEST, 2904, "리프레시 토큰이 아닙니다."),
    INVALID_ACCESS_TOKEN(false, HttpStatus.UNAUTHORIZED, 2905, "잘못된 access token 입니다."),
    INVALID_TOKEN_TYPE(false, HttpStatus.UNAUTHORIZED, 2906, "잘못된 토큰 타입입니다."),
    ACCESS_DENIED(false, HttpStatus.FORBIDDEN, 2951, "권한이 없는 유저의 접근입니다."),
    UNAUTHORIZED_ACCESS(false, HttpStatus.UNAUTHORIZED, 2952, "로그인이 필요합니다."),

    /**
     * 5000: 서버 예외
     */
    INTERNAL_SERVER_ERROR(false, HttpStatus.INTERNAL_SERVER_ERROR, 5000, "서버 내부 오류가 발생했습니다.");

    private final boolean isSuccess;

    @JsonIgnore
    private final HttpStatus httpStatus;

    private final int responseCode;
    private final String responseMessage;
}