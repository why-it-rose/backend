package com.whyitrose.apiserver.stock.ls;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class LsRealtimeQuoteHttpClient implements LsRealtimeQuoteClient {

    private static final String TR_CD = "t1101";
    private static final long MIN_INTERVAL_MS = 120L;

    private final ObjectMapper objectMapper;
    private final LsTokenManager lsTokenManager;
    private final RestTemplate restTemplate = new RestTemplate();
    private final Object throttleLock = new Object();
    private long lastRequestAtMillis = 0L;

    @Value("${LS_OPENAPI_BASE_URL:https://openapi.ls-sec.co.kr:8080}")
    private String baseUrl;

    @Value("${LS_MAC_ADDRESS:}")
    private String macAddress;

    @Override
    public LsRealtimeQuote fetchQuote(String shcode) {
        try {
            acquireSlot();
            String accessToken = lsTokenManager.getAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken.trim());
            headers.set("tr_cd", TR_CD);
            headers.set("tr_cont", "N");
            headers.set("tr_cont_key", "");
            if (macAddress != null && !macAddress.isBlank()) {
                headers.set("mac_address", macAddress.trim());
            }

            ObjectNode inBlock = objectMapper.createObjectNode();
            inBlock.put("shcode", normalizeShcode(shcode));
            ObjectNode body = objectMapper.createObjectNode();
            body.set("t1101InBlock", inBlock);

            String url = UriComponentsBuilder.fromUriString(baseUrl.replaceAll("/+$", "") + "/stock/market-data")
                    .build()
                    .toUriString();
            String json = objectMapper.writeValueAsString(body);
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(json, headers),
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            String rspCd = text(root, "rsp_cd");
            if (!"00000".equals(rspCd)) {
                throw new IllegalStateException("LS t1101 failed: " + rspCd + " " + text(root, "rsp_msg"));
            }

            JsonNode out = root.path("t1101OutBlock");
            return new LsRealtimeQuote(
                    text(out, "hname"),
                    parseLong(text(out, "price")),
                    parseLong(text(out, "change")),
                    parseDouble(text(out, "diff"))
            );
        } catch (Exception e) {
            throw new IllegalStateException("LS t1101 request failed", e);
        }
    }

    private void acquireSlot() throws InterruptedException {
        synchronized (throttleLock) {
            long now = System.currentTimeMillis();
            long waitMs = (lastRequestAtMillis + MIN_INTERVAL_MS) - now;
            if (waitMs > 0) {
                Thread.sleep(waitMs);
            }
            lastRequestAtMillis = System.currentTimeMillis();
        }
    }

    private String normalizeShcode(String shcode) {
        return shcode == null ? "" : shcode.trim();
    }

    private String text(JsonNode node, String key) {
        if (node == null || node.isMissingNode() || node.get(key) == null || node.get(key).isNull()) {
            return "";
        }
        return node.get(key).asText("");
    }

    private long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value.replace(",", "").trim());
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.replace(",", "").trim());
        } catch (Exception ignored) {
            return 0.0;
        }
    }
}
