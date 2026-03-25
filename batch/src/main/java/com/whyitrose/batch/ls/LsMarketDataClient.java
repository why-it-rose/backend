package com.whyitrose.batch.ls;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.whyitrose.batch.config.LsOpenApiProperties;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * LS OpenAPI 호출 (t9945 주식마스터, t8451 차트 등).
 * 연속조회는 응답 HTTP 헤더의 tr_cont, tr_cont_key를 사용합니다.
 * t8451은 {@link LsOpenApiProperties#getChartMinIntervalMs()}로 전역 스로틀(기본 1초 1건).
 */
@Slf4j
@Component
public class LsMarketDataClient {

    private static final String T9945 = "t9945";
    private static final String T1532 = "t1532";
    private static final String T8451 = "t8451";
    private static final String RATE_LIMIT_CODE = "IGW00201";
    private static final int CHART_RETRY_MAX_ATTEMPTS = 60;

    private final LsOpenApiProperties properties;
    private final LsTokenClient tokenClient;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    /** t8451 전역: 직전 요청 시작 시각 기준 다음 요청까지 최소 간격 */
    private final Object t8451ThrottleLock = new Object();
    private long lastT8451RequestStartMillis = 0L;

    public LsMarketDataClient(LsOpenApiProperties properties, LsTokenClient tokenClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.tokenClient = tokenClient;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    public List<JsonNode> fetchAllMasterRows(String gubun) throws InterruptedException {
        List<JsonNode> rows = new ArrayList<>();
        String sendCont = "N";
        String sendKey = "";
        boolean first = true;
        while (true) {
            if (!first) {
                Thread.sleep(Math.max(properties.getMinIntervalMs(), 500L));
            }
            first = false;
            ResponseEntity<String> response = postMarketData(gubun, sendCont, sendKey);
            String body = response.getBody();
            if (body == null || body.isBlank()) {
                throw new IllegalStateException("LS API 응답 body가 비어 있습니다. gubun=" + gubun);
            }
            JsonNode root;
            try {
                root = objectMapper.readTree(body);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("LS API JSON 파싱 실패 gubun=" + gubun, e);
            }
            assertOkResponse(root, gubun);
            List<JsonNode> page = extractOutBlockRows(root);
            for (JsonNode row : page) {
                rows.add(row);
            }
            String recvCont = headerOrBody(response, root, "tr_cont");
            String recvKey = headerOrBody(response, root, "tr_cont_key");
            if (recvCont == null || !"Y".equalsIgnoreCase(recvCont.trim())) {
                break;
            }
            sendCont = "Y";
            sendKey = recvKey == null ? "" : recvKey.trim();
            log.debug("t9945 연속조회 gubun={} tr_cont=Y keyLen={}", gubun, sendKey.length());
        }
        return rows;
    }

    public String fetchPrimaryThemeName(String shcode) {
        String base = properties.getBaseUrl().replaceAll("/+$", "");
        URI uri = UriComponentsBuilder.fromUriString(base + "/stock/sector")
                .build()
                .toUri();
        HttpHeaders headers = defaultHeaders(T1532, "N", "");

        ObjectNode inBlock = objectMapper.createObjectNode();
        inBlock.put("shcode", shcode);
        ObjectNode body = objectMapper.createObjectNode();
        body.set("t1532InBlock", inBlock);

        try {
            String json = objectMapper.writeValueAsString(body);
            ResponseEntity<String> response =
                    restTemplate.exchange(uri, HttpMethod.POST, new HttpEntity<>(json, headers), String.class);
            String responseBody = response.getBody();
            if (responseBody == null || responseBody.isBlank()) {
                return null;
            }
            JsonNode root = objectMapper.readTree(responseBody);
            assertOkResponse(root, shcode);
            JsonNode out = root.get("t1532OutBlock");
            if (out == null || !out.isArray() || out.isEmpty()) {
                return null;
            }
            JsonNode first = out.get(0);
            if (first == null || !first.has("tmname")) {
                return null;
            }
            String tmname = first.get("tmname").asText();
            return tmname == null ? null : tmname.trim();
        } catch (Exception e) {
            throw new IllegalStateException("t1532 요청 실패 shcode=" + shcode, e);
        }
    }

    public List<JsonNode> fetchAllChartRows(String shcode, String gubun, String exchgubun) throws InterruptedException {
        return fetchAllChartRows(shcode, gubun, exchgubun, "");
    }

    public List<JsonNode> fetchAllChartRows(String shcode, String gubun, String exchgubun, String sdate) throws InterruptedException {
        List<JsonNode> rows = new ArrayList<>();
        String sendCont = "N";
        String sendKey = "";
        String ctsDate = "";

        while (true) {
            ResponseEntity<String> response = postChartDataWithRetry(shcode, gubun, exchgubun, sendCont, sendKey, ctsDate, sdate);
            String body = response.getBody();
            if (body == null || body.isBlank()) {
                throw new IllegalStateException("LS API 응답 body가 비어 있습니다. shcode=" + shcode + ", gubun=" + gubun);
            }

            JsonNode root;
            try {
                root = objectMapper.readTree(body);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("LS API JSON 파싱 실패 shcode=" + shcode + ", gubun=" + gubun, e);
            }

            assertOkResponse(root, shcode + "/" + gubun);

            JsonNode outBlock1 = root.get("t8451OutBlock1");
            if (outBlock1 != null && outBlock1.isArray()) {
                outBlock1.forEach(rows::add);
            }

            JsonNode outBlock = root.get("t8451OutBlock");
            if (outBlock != null && outBlock.isObject()) {
                ctsDate = text(outBlock, "cts_date", "");
            }

            String recvCont = headerOrBody(response, root, "tr_cont");
            String recvKey = headerOrBody(response, root, "tr_cont_key");
            if (recvCont == null || !"Y".equalsIgnoreCase(recvCont.trim())) {
                break;
            }
            sendCont = "Y";
            sendKey = recvKey == null ? "" : recvKey.trim();
        }

        return rows;
    }

    private ResponseEntity<String> postMarketData(String gubun, String trCont, String trContKey) {
        String base = properties.getBaseUrl().replaceAll("/+$", "");
        URI uri = UriComponentsBuilder.fromUriString(base + "/stock/market-data")
                .build()
                .toUri();
        HttpHeaders headers = defaultHeaders(T9945, trCont, trContKey);

        ObjectNode inBlock = objectMapper.createObjectNode();
        inBlock.put("gubun", gubun);
        ObjectNode body = objectMapper.createObjectNode();
        body.set("t9945InBlock", inBlock);

        try {
            String json = objectMapper.writeValueAsString(body);
            return restTemplate.exchange(uri, HttpMethod.POST, new HttpEntity<>(json, headers), String.class);
        } catch (Exception e) {
            throw new IllegalStateException("t9945 요청 실패 gubun=" + gubun, e);
        }
    }

    private ResponseEntity<String> postChartData(
            String shcode,
            String gubun,
            String exchgubun,
            String trCont,
            String trContKey,
            String ctsDate,
            String sdate) throws InterruptedException {
        acquireT8451Slot();

        String base = properties.getBaseUrl().replaceAll("/+$", "");
        URI uri = UriComponentsBuilder.fromUriString(base + "/stock/chart")
                .build()
                .toUri();
        HttpHeaders headers = defaultHeaders(T8451, trCont, trContKey);

        ObjectNode inBlock = objectMapper.createObjectNode();
        inBlock.put("shcode", shcode);
        inBlock.put("gubun", gubun);
        inBlock.put("qrycnt", 500);
        inBlock.put("sdate", sdate == null ? "" : sdate);
        inBlock.put("edate", "99999999");
        inBlock.put("cts_date", ctsDate == null ? "" : ctsDate);
        inBlock.put("comp_yn", "N");
        inBlock.put("sujung", "Y");
        inBlock.put("exchgubun", exchgubun == null || exchgubun.isBlank() ? "K" : exchgubun);

        ObjectNode body = objectMapper.createObjectNode();
        body.set("t8451InBlock", inBlock);

        try {
            String json = objectMapper.writeValueAsString(body);
            return restTemplate.exchange(uri, HttpMethod.POST, new HttpEntity<>(json, headers), String.class);
        } catch (Exception e) {
            throw new IllegalStateException("t8451 요청 실패 shcode=" + shcode + ", gubun=" + gubun, e);
        }
    }

    /** t8451 전역: 종목·연속조회 구분 없이 직전 호출 시작 이후 chartMinIntervalMs 경과 후에만 다음 호출 */
    private void acquireT8451Slot() throws InterruptedException {
        synchronized (t8451ThrottleLock) {
            long minMs = Math.max(1L, properties.getChartMinIntervalMs());
            long now = System.currentTimeMillis();
            if (lastT8451RequestStartMillis > 0L) {
                long nextAllowed = lastT8451RequestStartMillis + minMs;
                long wait = nextAllowed - now;
                if (wait > 0L) {
                    Thread.sleep(wait);
                }
            }
            lastT8451RequestStartMillis = System.currentTimeMillis();
        }
    }

    private ResponseEntity<String> postChartDataWithRetry(
            String shcode,
            String gubun,
            String exchgubun,
            String trCont,
            String trContKey,
            String ctsDate,
            String sdate) throws InterruptedException {
        long baseDelayMs = Math.max(properties.getChartMinIntervalMs(), 1200L);
        long maxDelayMs = 10_000L;
        for (int attempt = 1; attempt <= CHART_RETRY_MAX_ATTEMPTS; attempt++) {
            try {
                return postChartData(shcode, gubun, exchgubun, trCont, trContKey, ctsDate, sdate);
            } catch (IllegalStateException ex) {
                if (!isRateLimitException(ex) || attempt == CHART_RETRY_MAX_ATTEMPTS) {
                    throw ex;
                }
                long sleepMs = Math.min(baseDelayMs * attempt, maxDelayMs);
                log.warn("t8451 rate-limit 감지, 대기 후 재시도: ticker={}, gubun={}, attempt={}/{}, sleepMs={}",
                        shcode, gubun, attempt, CHART_RETRY_MAX_ATTEMPTS, sleepMs);
                Thread.sleep(sleepMs);
            }
        }
        throw new IllegalStateException("t8451 재시도 로직이 비정상 종료되었습니다. shcode=" + shcode + ", gubun=" + gubun);
    }

    private boolean isRateLimitException(IllegalStateException ex) {
        if (ex == null) {
            return false;
        }
        String message = ex.getMessage();
        if (message != null && message.contains(RATE_LIMIT_CODE)) {
            return true;
        }
        Throwable cause = ex.getCause();
        if (cause instanceof HttpStatusCodeException httpEx) {
            String body = httpEx.getResponseBodyAsString();
            return body != null && body.contains(RATE_LIMIT_CODE);
        }
        return false;
    }

    private HttpHeaders defaultHeaders(String trCd, String trCont, String trContKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        headers.set("authorization", "Bearer " + tokenClient.getToken());
        headers.set("tr_cd", trCd);
        headers.set("tr_cont", trCont == null || trCont.isBlank() ? "N" : trCont);
        headers.set("tr_cont_key", trContKey == null ? "" : trContKey);
        return headers;
    }

    private void assertOkResponse(JsonNode root, String keyHint) {
        if (!root.has("rsp_cd")) {
            return;
        }
        String code = root.get("rsp_cd").asText("").trim();
        if (code.isEmpty() || "00000".equals(code) || "000000".equals(code)) {
            return;
        }
        String msg = root.has("rsp_msg") ? root.get("rsp_msg").asText() : "";
        throw new IllegalStateException("LS API 오류 key=" + keyHint + " rsp_cd=" + code + " rsp_msg=" + msg);
    }

    private List<JsonNode> extractOutBlockRows(JsonNode root) {
        List<JsonNode> list = new ArrayList<>();
        Iterator<String> names = root.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            if (!name.contains("OutBlock")) {
                continue;
            }
            JsonNode n = root.get(name);
            if (n == null || n.isNull()) {
                continue;
            }
            if (n.isArray()) {
                n.forEach(list::add);
            } else if (n.isObject()) {
                list.add(n);
            }
        }
        return list;
    }

    private String firstHeader(ResponseEntity<String> response, String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (String key : response.getHeaders().keySet()) {
            if (key != null && key.toLowerCase(Locale.ROOT).equals(lower)) {
                List<String> values = response.getHeaders().get(key);
                if (values != null && !values.isEmpty()) {
                    return values.get(0);
                }
            }
        }
        return null;
    }

    private String headerOrBody(ResponseEntity<String> response, JsonNode root, String field) {
        String fromHeader = firstHeader(response, field);
        if (fromHeader != null && !fromHeader.isBlank()) {
            return fromHeader;
        }
        if (root.has(field) && !root.get(field).isNull()) {
            return root.get(field).asText();
        }
        return null;
    }

    private String text(JsonNode node, String field, String defaultValue) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return defaultValue;
        }
        String value = node.get(field).asText();
        return value == null ? defaultValue : value.trim();
    }
}
