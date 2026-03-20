package com.whyitrose.apiserver.auth.controller;

import com.whyitrose.apiserver.auth.dto.LoginRequest;
import com.whyitrose.apiserver.auth.dto.LoginResponse;
import com.whyitrose.apiserver.auth.dto.SignupRequest;
import com.whyitrose.apiserver.auth.dto.UserResponse;
import com.whyitrose.apiserver.auth.service.AuthService;
import com.whyitrose.core.response.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<BaseResponse<UserResponse>> signup(@RequestBody @Valid SignupRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success(authService.signup(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<BaseResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(BaseResponse.success(authService.login(request)));
    }


}
