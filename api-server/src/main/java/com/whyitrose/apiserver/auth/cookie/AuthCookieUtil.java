package com.whyitrose.apiserver.auth.cookie;

import com.whyitrose.apiserver.auth.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthCookieUtil {

        public static final String ACCESS_TOKEN_COOKIE_NAME = "ACCESS_TOKEN";
        public static final String REFRESH_TOKEN_COOKIE_NAME = "REFRESH_TOKEN";

        private final JwtTokenProvider jwtTokenProvider;

        @Value("${app.auth.cookie.secure:false}")
        private boolean secure;

        @Value("${app.auth.cookie.same-site:Lax}")
        private String sameSite;

        public AuthCookieUtil(JwtTokenProvider jwtTokenProvider) {
            this.jwtTokenProvider = jwtTokenProvider;
        }
        // 쿠키 생성
        public ResponseCookie createAccessTokenCookie(String token) {
            return buildCookie(ACCESS_TOKEN_COOKIE_NAME, token, jwtTokenProvider.getAccessTokenExpirationMs());
        }

        public ResponseCookie createRefreshTokenCookie(String token) {
            return buildCookie(REFRESH_TOKEN_COOKIE_NAME, token, jwtTokenProvider.getRefreshTokenExpirationMs());
        }
        // 쿠키 삭제
        public ResponseCookie clearAccessTokenCookie() {
            return buildCookie(ACCESS_TOKEN_COOKIE_NAME, "", 0);
        }

        public ResponseCookie clearRefreshTokenCookie() {
            return buildCookie(REFRESH_TOKEN_COOKIE_NAME, "", 0);
        }

        private ResponseCookie buildCookie(String name, String value, long maxAgeMs) {
            long maxAgeSec = Math.max(0, maxAgeMs / 1000);
            // 일관된 쿠키 설정
            return ResponseCookie.from(name, value)
                    .httpOnly(true)
                    .secure(secure)
                    .path("/")
                    .sameSite(sameSite)
                    .maxAge(Duration.ofSeconds(maxAgeSec))
                    .build();
        }
}
