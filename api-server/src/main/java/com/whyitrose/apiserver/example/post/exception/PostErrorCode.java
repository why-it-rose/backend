package com.whyitrose.apiserver.example.post.exception;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.whyitrose.core.response.ResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 도메인별 에러 코드 예시입니다.
 * 실제 도메인 작업 시 이 패턴을 참고해 각 도메인 패키지 안에 정의하세요.
 * ResponseStatus 인터페이스를 구현하면 BaseResponse, BaseException과 함께 사용할 수 있습니다.
 */
@Getter
@AllArgsConstructor
public enum PostErrorCode implements ResponseStatus {

    POST_NOT_FOUND(false, HttpStatus.NOT_FOUND, 3001, "존재하지 않는 게시글입니다."),
    UNAUTHORIZED_POST_ACCESS(false, HttpStatus.FORBIDDEN, 3002, "해당 게시글에 대한 권한이 없습니다.");

    private final boolean isSuccess;

    @JsonIgnore
    private final HttpStatus httpStatus;

    private final int responseCode;
    private final String responseMessage;
}