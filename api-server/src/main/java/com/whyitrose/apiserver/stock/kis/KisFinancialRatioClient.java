package com.whyitrose.apiserver.stock.kis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class KisFinancialRatioClient {
    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.BASIC_ISO_DATE;

    private final ObjectMapper objectMapper;
    private final KisTokenManager kisTokenManager;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${KIS_OPENAPI_BASE_URL:https://openapi.koreainvestment.com:9443}")
    private String baseUrl;

    @Value("${KIS_APP_KEY:}")
    private String appKey;

    @Value("${KIS_APP_SECRET:}")
    private String appSecret;

    public KisFinancialRatio fetchYearlyRatio(String ticker) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString(baseUrl.replaceAll("/+$", "") + "/uapi/domestic-stock/v1/finance/financial-ratio")
                    .queryParam("FID_DIV_CLS_CODE", "0")
                    .queryParam("fid_cond_mrkt_div_code", "J")
                    .queryParam("fid_input_iscd", ticker)
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("content-type", "application/json; charset=utf-8");
            headers.setBearerAuth(kisTokenManager.getAccessToken());
            headers.set("appkey", appKey == null ? "" : appKey.trim());
            headers.set("appsecret", appSecret == null ? "" : appSecret.trim());
            headers.set("tr_id", "FHKST66430300");
            headers.set("custtype", "P");

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            String body = response.getBody();
            if (body == null || body.isBlank()) {
                throw new IllegalStateException("KIS financial ratio response is empty");
            }

            JsonNode root = objectMapper.readTree(body);
            if (!"0".equals(root.path("rt_cd").asText(""))) {
                throw new IllegalStateException("KIS financial ratio failed: " + root.path("msg_cd").asText("")
                        + " " + root.path("msg1").asText(""));
            }
            JsonNode output = root.path("output");
            if (!output.isArray() || output.isEmpty()) {
                return new KisFinancialRatio("", 0.0, 0.0, 0.0);
            }
            JsonNode row = output.get(0);
            return new KisFinancialRatio(
                    normalizeYyyyMm(row.path("stac_yymm").asText("")),
                    parseDouble(row.path("grs").asText("")),
                    parseDouble(row.path("bsop_prfi_inrt").asText("")),
                    parseDouble(row.path("ntin_inrt").asText(""))
            );
        } catch (Exception e) {
            throw new IllegalStateException("KIS financial ratio request failed", e);
        }
    }

    public KisIncomeStatement fetchYearlyIncomeStatement(String ticker) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString(baseUrl.replaceAll("/+$", "") + "/uapi/domestic-stock/v1/finance/income-statement")
                    .queryParam("FID_DIV_CLS_CODE", "0")
                    .queryParam("fid_cond_mrkt_div_code", "J")
                    .queryParam("fid_input_iscd", ticker)
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("content-type", "application/json; charset=utf-8");
            headers.setBearerAuth(kisTokenManager.getAccessToken());
            headers.set("appkey", appKey == null ? "" : appKey.trim());
            headers.set("appsecret", appSecret == null ? "" : appSecret.trim());
            headers.set("tr_id", "FHKST66430200");
            headers.set("custtype", "P");

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            String body = response.getBody();
            if (body == null || body.isBlank()) {
                throw new IllegalStateException("KIS income statement response is empty");
            }

            JsonNode root = objectMapper.readTree(body);
            if (!"0".equals(root.path("rt_cd").asText(""))) {
                throw new IllegalStateException("KIS income statement failed: " + root.path("msg_cd").asText("")
                        + " " + root.path("msg1").asText(""));
            }
            JsonNode output = root.path("output");
            if (!output.isArray() || output.isEmpty()) {
                return new KisIncomeStatement("", 0L, 0L, 0L, 0L);
            }
            JsonNode row = output.get(0);
            JsonNode previousRow = output.size() > 1 ? output.get(1) : null;
            return new KisIncomeStatement(
                    normalizeYyyyMm(row.path("stac_yymm").asText("")),
                    parseLong(row.path("sale_account").asText("")),
                    previousRow == null ? 0L : parseLong(previousRow.path("sale_account").asText("")),
                    parseLong(row.path("bsop_prti").asText("")),
                    parseLong(row.path("thtr_ntin").asText(""))
            );
        } catch (Exception e) {
            throw new IllegalStateException("KIS income statement request failed", e);
        }
    }

    public KisInvestorTrading fetchInvestorTradingDaily(String ticker) {
        LocalDate date = LocalDate.now();
        IllegalStateException lastException = null;
        for (int i = 0; i < 7; i++) {
            try {
                KisInvestorTrading trading = fetchInvestorTradingByDate(ticker, date.minusDays(i));
                if (!trading.baseDate().isBlank()) {
                    return trading;
                }
            } catch (IllegalStateException e) {
                lastException = e;
            }
        }
        if (lastException != null) {
            throw lastException;
        }
        return new KisInvestorTrading("", 0L, 0L, 0L);
    }

    public KisStockBasicInfo fetchStockBasicInfo(String ticker) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString(baseUrl.replaceAll("/+$", "") + "/uapi/domestic-stock/v1/quotations/search-stock-info")
                    .queryParam("PRDT_TYPE_CD", "300")
                    .queryParam("PDNO", ticker)
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("content-type", "application/json; charset=utf-8");
            headers.setBearerAuth(kisTokenManager.getAccessToken());
            headers.set("appkey", appKey == null ? "" : appKey.trim());
            headers.set("appsecret", appSecret == null ? "" : appSecret.trim());
            headers.set("tr_id", "CTPF1002R");
            headers.set("custtype", "P");

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            String body = response.getBody();
            if (body == null || body.isBlank()) {
                throw new IllegalStateException("KIS stock basic info response is empty");
            }

            JsonNode root = objectMapper.readTree(body);
            if (!"0".equals(root.path("rt_cd").asText(""))) {
                throw new IllegalStateException("KIS stock basic info failed: " + root.path("msg_cd").asText("")
                        + " " + root.path("msg1").asText(""));
            }
            JsonNode output = root.path("output");
            if (!output.isObject() || output.isEmpty()) {
                return new KisStockBasicInfo("");
            }
            return new KisStockBasicInfo(output.path("idx_bztp_scls_cd_name").asText("").trim());
        } catch (Exception e) {
            throw new IllegalStateException("KIS stock basic info request failed", e);
        }
    }

    private KisInvestorTrading fetchInvestorTradingByDate(String ticker, LocalDate date) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString(baseUrl.replaceAll("/+$", "") + "/uapi/domestic-stock/v1/quotations/investor-trade-by-stock-daily")
                    .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                    .queryParam("FID_INPUT_ISCD", ticker)
                    .queryParam("FID_INPUT_DATE_1", YYYYMMDD.format(date))
                    .queryParam("FID_ORG_ADJ_PRC", "")
                    .queryParam("FID_ETC_CLS_CODE", "1")
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("content-type", "application/json; charset=utf-8");
            headers.setBearerAuth(kisTokenManager.getAccessToken());
            headers.set("appkey", appKey == null ? "" : appKey.trim());
            headers.set("appsecret", appSecret == null ? "" : appSecret.trim());
            headers.set("tr_id", "FHPTJ04160001");
            headers.set("custtype", "P");

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            String body = response.getBody();
            if (body == null || body.isBlank()) {
                throw new IllegalStateException("KIS investor trading response is empty");
            }

            JsonNode root = objectMapper.readTree(body);
            if (!"0".equals(root.path("rt_cd").asText(""))) {
                throw new IllegalStateException("KIS investor trading failed: " + root.path("msg_cd").asText("")
                        + " " + root.path("msg1").asText(""));
            }

            JsonNode output = root.path("output2");
            if (!output.isArray() || output.isEmpty()) {
                return new KisInvestorTrading("", 0L, 0L, 0L);
            }

            JsonNode row = output.get(0);
            return new KisInvestorTrading(
                    normalizeYyyyMmDd(row.path("stck_bsop_date").asText("")),
                    parseLong(row.path("frgn_ntby_qty").asText("")),
                    parseLong(row.path("orgn_ntby_qty").asText("")),
                    parseLong(row.path("prsn_ntby_qty").asText(""))
            );
        } catch (Exception e) {
            throw new IllegalStateException("KIS investor trading request failed", e);
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

    private long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            // KIS 재무 API는 금액 필드를 "24986.00"처럼 소수점 문자열로 내려준다.
            return new BigDecimal(value.replace(",", "").trim()).longValue();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private String normalizeYyyyMm(String value) {
        if (value == null) {
            return "";
        }
        String v = value.trim();
        if (v.length() == 6) {
            return v.substring(0, 4) + "-" + v.substring(4);
        }
        return v;
    }

    private String normalizeYyyyMmDd(String value) {
        if (value == null) {
            return "";
        }
        String v = value.trim();
        if (v.length() == 8) {
            return v.substring(0, 4) + "-" + v.substring(4, 6) + "-" + v.substring(6);
        }
        return v;
    }
}
