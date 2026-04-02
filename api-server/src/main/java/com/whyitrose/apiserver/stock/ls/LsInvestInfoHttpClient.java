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
public class LsInvestInfoHttpClient implements LsInvestInfoClient {

    private static final String TR_CD = "t3320";
    private static final long MIN_INTERVAL_MS = 1000L;

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
    public LsCompanyInfo fetchCompanyInfo(String ticker) {
        try {
            acquireSlot();
            String accessToken = lsTokenManager.getAccessToken();
            String gicode = normalizeGicode(ticker);
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
            inBlock.put("gicode", gicode);
            ObjectNode body = objectMapper.createObjectNode();
            body.set("t3320InBlock", inBlock);

            String url = UriComponentsBuilder.fromUriString(baseUrl.replaceAll("/+$", "") + "/stock/investinfo")
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
                throw new IllegalStateException("LS t3320 failed: " + rspCd + " " + text(root, "rsp_msg"));
            }

            JsonNode out = root.path("t3320OutBlock");
            LsCompanyInfo companyInfo = new LsCompanyInfo(
                    text(out, "upgubunnm"),
                    parseLong(text(out, "sigavalue")),
                    parseLong(text(out, "gstock")),
                    parseDouble(text(out, "foreignratio"))
            );
            if (companyInfo.industryGroup().isBlank()
                    && companyInfo.marketCap() == 0L
                    && companyInfo.totalShares() == 0L
                    && companyInfo.foreignRatio() == 0.0) {
                throw new IllegalStateException("LS t3320 returned empty payload for gicode=" + gicode);
            }
            return companyInfo;
        } catch (Exception e) {
            throw new IllegalStateException("LS t3320 request failed", e);
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

    private String normalizeGicode(String ticker) {
        String value = ticker == null ? "" : ticker.trim();
        if (value.startsWith("A") || value.startsWith("a")) {
            return value.substring(1);
        }
        return value;
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
