package com.whyitrose.apiserver.stock.kis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
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
public class KisTokenManager {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${KIS_OPENAPI_BASE_URL:https://openapi.koreainvestment.com:9443}")
    private String baseUrl;

    @Value("${KIS_APP_KEY:}")
    private String appKey;

    @Value("${KIS_APP_SECRET:}")
    private String appSecret;

    private volatile String accessToken;
    private volatile Instant expiresAt;

    public String getAccessToken() {
        String token = accessToken;
        Instant exp = expiresAt;
        if (token != null && exp != null && Instant.now().isBefore(exp.minusSeconds(300))) {
            return token;
        }
        synchronized (this) {
            if (accessToken == null || expiresAt == null || Instant.now().isAfter(expiresAt.minusSeconds(300))) {
                issueToken();
            }
            return accessToken;
        }
    }

    public synchronized void issueToken() {
        if (appKey == null || appKey.isBlank() || appSecret == null || appSecret.isBlank()) {
            throw new IllegalStateException("KIS credentials are missing. Set KIS_APP_KEY and KIS_APP_SECRET.");
        }
        try {
            String url = baseUrl.replaceAll("/+$", "") + "/oauth2/tokenP";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String body = """
                    {
                      "grant_type":"client_credentials",
                      "appkey":"%s",
                      "appsecret":"%s"
                    }
                    """.formatted(appKey.trim(), appSecret.trim());

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

            String responseBody = response.getBody();
            if (responseBody == null || responseBody.isBlank()) {
                throw new IllegalStateException("KIS token response is empty");
            }
            JsonNode root = objectMapper.readTree(responseBody);
            String token = root.path("access_token").asText("");
            long expiresIn = root.path("expires_in").asLong(86400L);
            if (token.isBlank()) {
                throw new IllegalStateException("KIS token response missing access_token: " + responseBody);
            }
            accessToken = token.trim();
            expiresAt = Instant.now().plusSeconds(Math.max(expiresIn, 300L));
            log.info("KIS access token issued");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to issue KIS access token", e);
        }
    }
}
