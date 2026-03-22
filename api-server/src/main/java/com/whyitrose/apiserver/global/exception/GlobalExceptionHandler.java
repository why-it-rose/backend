package com.whyitrose.apiserver.global.exception;

import com.whyitrose.apiserver.auth.exception.AuthErrorCode;
import com.whyitrose.core.exception.BaseException;
import com.whyitrose.core.response.BaseResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<BaseResponse<Void>> handleBaseException(BaseException e) {
        return ResponseEntity
                .status(e.getStatus().getHttpStatus())
                .body(BaseResponse.failure(e.getStatus()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<String>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse(AuthErrorCode.AUTH_009.getResponseMessage());

        return ResponseEntity
                .status(AuthErrorCode.AUTH_009.getHttpStatus())
                .body(BaseResponse.failure(AuthErrorCode.AUTH_009, message));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<BaseResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.badRequest().body(BaseResponse.failure(AuthErrorCode.AUTH_009));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleException(Exception e) {
        return ResponseEntity
                .status(CommonErrorCode.COMMON_001.getHttpStatus())
                .body(BaseResponse.failure(CommonErrorCode.COMMON_001));
    }
}