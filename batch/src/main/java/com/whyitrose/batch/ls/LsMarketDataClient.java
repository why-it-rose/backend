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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * LS OpenAPI stock/market-data 호출 (t9945 주식마스터).
 * 연속조회는 응답 HTTP 헤더의 tr_cont, tr_cont_key를 사용합니다.
 */
@Slf4j
@Component
public class LsMarketDataClient {

    private static final String T9945 = "t9945";
    private static final String T1532 = "t1532";

    private final LsOpenApiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public LsMarketDataClient(LsOpenApiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
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

    private HttpHeaders defaultHeaders(String trCd, String trCont, String trContKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        headers.set("authorization", "Bearer " + properties.getAccessToken().trim());
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
}
