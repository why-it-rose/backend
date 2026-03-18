package com.whyitrose.apiserver.global.exception;

import com.whyitrose.core.exception.BaseException;
import com.whyitrose.core.response.BaseResponse;
import com.whyitrose.core.response.BaseResponseStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // BaseException — BaseResponseStatus 또는 도메인 에러 코드로 던진 예외
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<BaseResponse<Void>> handleBaseException(BaseException e) {
        return ResponseEntity
                .status(e.getStatus().getHttpStatus())
                .body(BaseResponse.failure(e.getStatus()));
    }

    // @Valid 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse(BaseResponseStatus.METHOD_ARGUMENT_TYPE_MISMATCH.getResponseMessage());

        return ResponseEntity
                .status(BaseResponseStatus.METHOD_ARGUMENT_TYPE_MISMATCH.getHttpStatus())
                .body(BaseResponse.failure(BaseResponseStatus.METHOD_ARGUMENT_TYPE_MISMATCH));
    }

    // 지원하지 않는 HTTP Method
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<BaseResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity
                .status(BaseResponseStatus.HTTP_METHOD_TYPE_MISMATCH.getHttpStatus())
                .body(BaseResponse.failure(BaseResponseStatus.HTTP_METHOD_TYPE_MISMATCH));
    }

    // 그 외 처리되지 않은 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleException(Exception e) {
        return ResponseEntity
                .status(BaseResponseStatus.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(BaseResponse.failure(BaseResponseStatus.INTERNAL_SERVER_ERROR));
    }
}