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
import com.whyitrose.apiserver.auth.dto.UpdateMeRequest;
import com.whyitrose.apiserver.auth.dto.UpdatePushEnabledRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private static final String NO_STORE_CACHE_CONTROL = "no-store, no-cache, must-revalidate, max-age=0";
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

    @GetMapping("/me")
    public ResponseEntity<BaseResponse<LoginResponse>> me(Authentication authentication) {
        Long userId = extractPrincipalUserId(authentication);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, NO_STORE_CACHE_CONTROL)
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(BaseResponse.success(authService.getMe(userId)));
    }

    @PatchMapping("/me")
    public ResponseEntity<BaseResponse<UserResponse>> updateMe(
            Authentication authentication,
            @RequestBody @Valid UpdateMeRequest request
    ) {
        Long userId = extractPrincipalUserId(authentication);
        return ResponseEntity.ok(BaseResponse.success(authService.updateMe(userId, request)));
    }

    @DeleteMapping("/me")
    public ResponseEntity<BaseResponse<String>> deleteMe(Authentication authentication) {
        Long userId = extractPrincipalUserId(authentication);
        authService.deleteMe(userId);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieUtil.clearAccessTokenCookie().toString())
                .header(HttpHeaders.SET_COOKIE, authCookieUtil.clearRefreshTokenCookie().toString())
                .body(BaseResponse.success("회원 탈퇴가 완료되었습니다."));
    }

    // 로그인/리프레시 성공 시 토큰은 쿠키로만 전달하고, 바디에는 사용자 정보만 반환
    private ResponseEntity<BaseResponse<LoginResponse>> withAuthCookies(AuthResult result) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieUtil.createAccessTokenCookie(result.accessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, authCookieUtil.createRefreshTokenCookie(result.refreshToken()).toString())
                .header(HttpHeaders.CACHE_CONTROL, NO_STORE_CACHE_CONTROL)
                .header("Pragma", "no-cache")
                .header("Expires", "0")
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

    @PatchMapping("/me/push-enabled")
    public ResponseEntity<BaseResponse<Void>> updatePushEnabled(
            Authentication authentication,
            @RequestBody @Valid UpdatePushEnabledRequest request
    ) {
        Long userId = extractPrincipalUserId(authentication);
        authService.updatePushEnabled(userId, request);
        return ResponseEntity.ok(BaseResponse.success(null));
    }

}
