package com.whyitrose.batch.ls;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whyitrose.batch.config.LsOpenApiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * LS OpenAPI OAuth2 토큰 발급.
 * appKey + appSecret이 설정된 경우 자동 발급하며, 없으면 정적 accessToken을 반환한다.
 */
@Slf4j
@Component
public class LsTokenClient {

    private static final String TOKEN_PATH = "/oauth2/token";

    private final LsOpenApiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    private volatile String cachedToken = null;

    public LsTokenClient(LsOpenApiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    /**
     * 유효한 액세스 토큰을 반환한다.
     * appKey + appSecret이 설정된 경우 LS API에서 발급하고, 아니면 정적 accessToken을 사용한다.
     */
    public String getToken() {
        if (cachedToken != null) {
            return cachedToken;
        }
        synchronized (this) {
            if (cachedToken != null) {
                return cachedToken;
            }
            cachedToken = resolveToken();
            return cachedToken;
        }
    }

    private String resolveToken() {
        String appKey = properties.getAppKey();
        String appSecret = properties.getAppSecret();

        if (appKey != null && !appKey.isBlank() && appSecret != null && !appSecret.isBlank()) {
            log.info("LS OpenAPI 토큰 자동 발급 시작 (appKey 사용)");
            String token = fetchToken(appKey, appSecret);
            log.info("LS OpenAPI 토큰 발급 완료");
            return token;
        }

        String staticToken = properties.getAccessToken();
        if (staticToken != null && !staticToken.isBlank()) {
            log.warn("LS_APP_KEY/LS_APP_SECRET 미설정 — 정적 LS_ACCESS_TOKEN을 사용합니다. 매일 만료되므로 앱 키 설정을 권장합니다.");
            return staticToken;
        }

        throw new IllegalStateException(
                "LS OpenAPI 인증 정보가 없습니다. LS_APP_KEY + LS_APP_SECRET 또는 LS_ACCESS_TOKEN을 설정하세요.");
    }

    private String fetchToken(String appKey, String appSecret) {
        String url = properties.getBaseUrl().replaceAll("/+$", "") + TOKEN_PATH;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/x-www-form-urlencoded");

        String body = "grant_type=client_credentials"
                + "&appkey=" + appKey
                + "&appsecretkey=" + appSecret
                + "&scope=oob";

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

            String responseBody = response.getBody();
            if (responseBody == null || responseBody.isBlank()) {
                throw new IllegalStateException("토큰 발급 응답이 비어 있습니다.");
            }

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode tokenNode = root.get("access_token");
            if (tokenNode == null || tokenNode.isNull() || tokenNode.asText().isBlank()) {
                throw new IllegalStateException("토큰 발급 응답에 access_token이 없습니다. response=" + responseBody);
            }

            return tokenNode.asText().trim();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("LS OpenAPI 토큰 발급 실패", e);
        }
    }
}
