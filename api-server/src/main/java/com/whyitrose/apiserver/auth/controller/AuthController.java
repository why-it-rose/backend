package com.whyitrose.apiserver.auth.controller;

import com.whyitrose.apiserver.auth.cookie.AuthCookieUtil;
import com.whyitrose.apiserver.auth.dto.AuthResult;
import com.whyitrose.apiserver.auth.dto.LoginRequest;
import com.whyitrose.apiserver.auth.dto.LoginResponse;
import com.whyitrose.apiserver.auth.dto.SignupRequest;
import com.whyitrose.apiserver.auth.dto.UserResponse;
import com.whyitrose.apiserver.auth.service.AuthService;
import com.whyitrose.core.response.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieUtil authCookieUtil;

    @PostMapping("/signup")
    public ResponseEntity<BaseResponse<UserResponse>> signup(@RequestBody @Valid SignupRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success(authService.signup(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<BaseResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest request) {
        AuthResult result = authService.login(request);
        return withAuthCookies(result);
    }

    @PostMapping("/refresh")
    public ResponseEntity<BaseResponse<LoginResponse>> refresh(
            @CookieValue(name = AuthCookieUtil.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken
    ) {
        AuthResult result = authService.refresh(refreshToken);
        return withAuthCookies(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<String>> logout(
            Authentication authentication,
            @CookieValue(name = AuthCookieUtil.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken
    ) {
        Long userId = extractPrincipalUserId(authentication);
        authService.logout(userId, refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieUtil.clearAccessTokenCookie().toString())
                .header(HttpHeaders.SET_COOKIE, authCookieUtil.clearRefreshTokenCookie().toString())
                .body(BaseResponse.success("로그아웃 되었습니다."));
    }

    // 로그인/리프레시 성공 시 토큰은 쿠키로만 전달하고, 바디에는 사용자 정보만 반환
    private ResponseEntity<BaseResponse<LoginResponse>> withAuthCookies(AuthResult result) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieUtil.createAccessTokenCookie(result.accessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, authCookieUtil.createRefreshTokenCookie(result.refreshToken()).toString())
                .body(BaseResponse.success(result.toLoginResponse()));
    }

    private Long extractPrincipalUserId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        if (principal instanceof Integer userId) {
            return userId.longValue();
        }
        return null;
    }
}
