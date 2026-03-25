package com.whyitrose.apiserver.auth.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whyitrose.apiserver.auth.dto.LoginResponse;
import com.whyitrose.apiserver.auth.exception.AuthErrorCode;
import com.whyitrose.apiserver.auth.service.AuthService;
import com.whyitrose.core.exception.BaseException;
import com.whyitrose.core.response.BaseResponse;
import com.whyitrose.domain.user.AuthProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final ObjectMapper objectMapper;
    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();

        try {
            if (principal.getEmail() == null || principal.getEmail().isBlank()) {
                throw new BaseException(AuthErrorCode.AUTH_014);
            }

            AuthProvider provider = switch (principal.getRegistrationId().toLowerCase()) {
                case "google" -> AuthProvider.GOOGLE;
                case "kakao" -> AuthProvider.KAKAO;
                default -> throw new BaseException(AuthErrorCode.AUTH_016);
            };

            LoginResponse result = authService.loginOrRegisterSocial(
                    provider,
                    principal.getProviderUid(),
                    principal.getEmail(),
                    principal.getName()
            );
            // accessToken, refreshToken, nickname을 만든 뒤 프론트로 리다이렉트
            String redirectUrl = String.format(
                    "%s/home#accessToken=%s&refreshToken=%s&nickname=%s",
                    frontendBaseUrl,
                    URLEncoder.encode(result.accessToken(), StandardCharsets.UTF_8),
                    URLEncoder.encode(result.refreshToken(), StandardCharsets.UTF_8),
                    URLEncoder.encode(result.nickname(), StandardCharsets.UTF_8)
            );
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);

        } catch (BaseException e) {
            response.setStatus(e.getStatus().getHttpStatus().value());
            response.setContentType("application/json;charset=UTF-8");
            objectMapper.writeValue(response.getWriter(), BaseResponse.failure(e.getStatus()));
        }
    }
}
