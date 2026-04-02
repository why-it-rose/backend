package com.whyitrose.apiserver.stock.ls;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class LsTokenManager {

    private static final String TOKEN_PATH = "/oauth2/token";

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${LS_OPENAPI_BASE_URL:https://openapi.ls-sec.co.kr:8080}")
    private String baseUrl;

    @Value("${LS_APP_KEY:}")
    private String appKey;

    @Value("${LS_APP_SECRET:}")
    private String appSecret;

    @Value("${LS_ACCESS_TOKEN:}")
    private String fallbackToken;

    private volatile String accessToken;

    public String getAccessToken() {
        String token = accessToken;
        if (token != null && !token.isBlank()) {
            return token;
        }
        synchronized (this) {
            if (accessToken == null || accessToken.isBlank()) {
                reissueToken();
            }
            return accessToken;
        }
    }

    public synchronized void reissueToken() {
        if (hasAppCredentials()) {
            accessToken = fetchTokenByCredentials();
            log.info("LS access token issued at {}", LocalDateTime.now());
            return;
        }
        if (fallbackToken != null && !fallbackToken.isBlank()) {
            accessToken = fallbackToken.trim();
            log.warn("LS_APP_KEY/LS_APP_SECRET not set. Using static LS_ACCESS_TOKEN.");
            return;
        }
        throw new IllegalStateException("LS credentials are missing. Set LS_APP_KEY/LS_APP_SECRET or LS_ACCESS_TOKEN.");
    }

    private boolean hasAppCredentials() {
        return appKey != null && !appKey.isBlank() && appSecret != null && !appSecret.isBlank();
    }

    private String fetchTokenByCredentials() {
        try {
            String url = baseUrl.replaceAll("/+$", "") + TOKEN_PATH;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = "grant_type=client_credentials"
                    + "&appkey=" + appKey.trim()
                    + "&appsecretkey=" + appSecret.trim()
                    + "&scope=oob";

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );
            String responseBody = response.getBody();
            if (responseBody == null || responseBody.isBlank()) {
                throw new IllegalStateException("LS token response is empty");
            }
            JsonNode root = objectMapper.readTree(responseBody);
            String token = root.path("access_token").asText("");
            if (token.isBlank()) {
                throw new IllegalStateException("LS token response missing access_token: " + responseBody);
            }
            return token.trim();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to issue LS access token", e);
        }
    }
}
