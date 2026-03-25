package com.whyitrose.apiserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whyitrose.core.response.BaseResponse;
import com.whyitrose.core.response.BaseResponseStatus;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SecurityAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        BaseResponseStatus status = BaseResponseStatus.UNAUTHORIZED_ACCESS;
        response.setStatus(status.getHttpStatus().value()); // 401
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), BaseResponse.failure(status));
    }
}
