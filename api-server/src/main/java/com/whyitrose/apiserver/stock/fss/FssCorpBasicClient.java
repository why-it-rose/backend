package com.whyitrose.apiserver.stock.fss;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class FssCorpBasicClient {
    private static final Pattern LATIN_RUN_PATTERN = Pattern.compile("[A-Za-z]+");
    private static final Map<Character, String> LETTER_TO_KOREAN = createLetterToKoreanMap();
    private static final Map<String, String> WORD_ALIAS = createWordAliasMap();

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${FSS_BASE_URL:http://apis.data.go.kr/1160100/service/GetCorpBasicInfoService_V2}")
    private String baseUrl;

    @Value("${FSS_SERVICE_KEY:}")
    private String serviceKey;

    public FssCompanyProfile fetchCompanyProfile(String corpName, String industryGroup) {
        String cleanedCorpName = normalizeCorpName(corpName);
        String cleanedIndustryGroup = normalizeIndustryGroup(industryGroup);
        if (serviceKey == null || serviceKey.isBlank()) {
            log.warn("FSS service key is blank. Falling back to industryGroup. corpName={}", cleanedCorpName);
            return new FssCompanyProfile(
                    fallbackOverview(cleanedCorpName, cleanedIndustryGroup, "", List.of(), List.of()),
                    cleanedIndustryGroup
            );
        }

        String establishedYear = "";
        String mainBiz = cleanedIndustryGroup;
        List<String> affiliates = List.of();
        List<String> subsidiaries = List.of();
        String corpRegistrationNo = "";

        try {
            JsonNode matched = resolveOutlineMatch(cleanedCorpName);
            if (matched != null && !matched.isMissingNode()) {
                corpRegistrationNo = text(matched, "crno");
                establishedYear = yearFromDate(text(matched, "enpEstbDt"));
                String apiMainBiz = text(matched, "enpMainBizNm");
                if (apiMainBiz.isBlank()) {
                    apiMainBiz = text(matched, "sicNm");
                }
                if (!apiMainBiz.isBlank()) {
                    mainBiz = apiMainBiz;
                }
            } else {
                log.warn("FSS outline match not found. corpName={}", cleanedCorpName);
            }
        } catch (Exception e) {
            log.warn("FSS outline lookup failed. corpName={}", cleanedCorpName, e);
        }

        if (!corpRegistrationNo.isBlank()) {
            try {
                JsonNode aff = callApi("getAffiliate_V2", Map.of("crno", corpRegistrationNo));
                affiliates = extractList(aff, "afilCmpyNm");
            } catch (Exception e) {
                log.warn("FSS affiliate lookup failed. corpName={}, crno={}", cleanedCorpName, corpRegistrationNo, e);
                affiliates = List.of();
            }

            try {
                JsonNode subs = callApi("getConsSubsComp_V2", Map.of("crno", corpRegistrationNo));
                subsidiaries = extractList(subs, "sbrdEnpNm");
            } catch (Exception e) {
                log.warn("FSS subsidiary lookup failed. corpName={}, crno={}", cleanedCorpName, corpRegistrationNo, e);
                subsidiaries = List.of();
            }
        } else {
            log.warn("FSS affiliate/subsidiary lookup skipped because crno is blank. corpName={}", cleanedCorpName);
        }

        log.info(
                "FSS company profile resolved. corpName={}, crno={}, year={}, mainBiz={}, affiliates={}, subsidiaries={}",
                cleanedCorpName,
                corpRegistrationNo,
                establishedYear,
                mainBiz,
                affiliates.size(),
                subsidiaries.size()
        );

        return new FssCompanyProfile(
                fallbackOverview(cleanedCorpName, mainBiz, establishedYear, affiliates, subsidiaries),
                mainBiz
        );
    }

    private JsonNode callApi(String endpoint, String corpName) throws Exception {
        return callApi(endpoint, Map.of("corpNm", corpName));
    }

    private JsonNode callApi(String endpoint, Map<String, String> queryParams) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("serviceKey", serviceKey.trim());
        params.put("pageNo", "1");
        params.put("numOfRows", "100");
        params.put("resultType", "json");
        params.putAll(queryParams);

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(baseUrl.replaceAll("/+$", "") + "/" + endpoint);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isBlank()) {
                builder.queryParam(entry.getKey(), entry.getValue());
            }
        }
        String url = builder.build(false).toUriString();

        return executeGet(url);
    }

    private List<String> extractList(JsonNode root, String key) {
        JsonNode item = root.path("response").path("body").path("items").path("item");
        List<String> result = new ArrayList<>();
        if (item.isArray()) {
            for (JsonNode node : item) {
                String v = normalizeCorpName(text(node, key));
                if (!v.isBlank() && !result.contains(v)) {
                    result.add(v);
                }
            }
        } else {
            String v = normalizeCorpName(text(item, key));
            if (!v.isBlank()) {
                result.add(v);
            }
        }
        return result.stream().limit(4).toList();
    }

    private JsonNode selectBestItem(JsonNode root, String expectedCorpName) {
        JsonNode item = root.path("response").path("body").path("items").path("item");
        String normalizedExpected = normalizeCorpName(expectedCorpName);
        if (item.isArray()) {
            JsonNode fuzzyMatched = null;
            for (JsonNode node : item) {
                if (matchesCorpName(node, normalizedExpected)) {
                    return node;
                }
                if (fuzzyMatched == null && looselyMatchesCorpName(node, normalizedExpected)) {
                    fuzzyMatched = node;
                }
            }
            return fuzzyMatched == null ? objectMapper.createObjectNode() : fuzzyMatched;
        }
        if (matchesCorpName(item, normalizedExpected) || looselyMatchesCorpName(item, normalizedExpected)) {
            return item;
        }
        return objectMapper.createObjectNode();
    }

    private boolean matchesCorpName(JsonNode node, String normalizedExpected) {
        if (node == null || node.isMissingNode() || normalizedExpected == null || normalizedExpected.isBlank()) {
            return false;
        }
        String corpNm = normalizeCorpName(text(node, "corpNm"));
        String publicName = normalizeCorpName(text(node, "enpPbanCmpyNm"));
        String expectedCanonical = canonicalCorpName(normalizedExpected);
        return normalizedExpected.equals(corpNm)
                || normalizedExpected.equals(publicName)
                || (!expectedCanonical.isBlank() && expectedCanonical.equals(canonicalCorpName(corpNm)))
                || (!expectedCanonical.isBlank() && expectedCanonical.equals(canonicalCorpName(publicName)));
    }

    private boolean looselyMatchesCorpName(JsonNode node, String normalizedExpected) {
        if (node == null || node.isMissingNode() || normalizedExpected == null || normalizedExpected.isBlank()) {
            return false;
        }
        String expected = canonicalCorpName(normalizedExpected);
        String corpNm = canonicalCorpName(normalizeCorpName(text(node, "corpNm")));
        String publicName = canonicalCorpName(normalizeCorpName(text(node, "enpPbanCmpyNm")));
        return expected.equals(corpNm)
                || expected.equals(publicName)
                || (!corpNm.isBlank() && corpNm.contains(expected))
                || (!publicName.isBlank() && publicName.contains(expected));
    }

    private JsonNode resolveOutlineMatch(String cleanedCorpName) throws Exception {
        for (String candidate : buildSearchNames(cleanedCorpName)) {
            for (int page = 1; page <= 5; page++) {
                JsonNode matched = selectBestItem(
                        callApi("getCorpOutline_V2", Map.of(
                                "corpNm", candidate,
                                "pageNo", String.valueOf(page)
                        )),
                        cleanedCorpName
                );
                if (hasResolvedCorp(matched)) {
                    return matched;
                }
            }
        }
        for (String candidate : buildSearchNames(cleanedCorpName)) {
            for (int page = 1; page <= 3; page++) {
                JsonNode matched = selectBestItem(
                        callApi("getCorpOutline_V2", Map.of(
                                "fnccmpNm", candidate,
                                "pageNo", String.valueOf(page)
                        )),
                        cleanedCorpName
                );
                if (hasResolvedCorp(matched)) {
                    return matched;
                }
            }
        }
        return objectMapper.createObjectNode();
    }

    private JsonNode executeGet(String url) throws Exception {
        Exception last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        url, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);
                String body = response.getBody();
                if (body == null || body.isBlank()) {
                    throw new IllegalStateException("FSS empty response");
                }
                JsonNode root = objectMapper.readTree(body);
                JsonNode header = root.path("response").path("header");
                if (!"00".equals(header.path("resultCode").asText(""))) {
                    throw new IllegalStateException("FSS response not ok: " + header.path("resultMsg").asText(""));
                }
                return root;
            } catch (HttpServerErrorException.GatewayTimeout e) {
                last = e;
                log.warn("FSS gateway timeout. attempt={}, url={}", attempt, url);
                Thread.sleep(500L * attempt);
            } catch (Exception e) {
                last = e;
                if (attempt == 3) {
                    throw e;
                }
                log.warn("FSS request retry. attempt={}, url={}", attempt, url, e);
                Thread.sleep(300L * attempt);
            }
        }
        throw last == null ? new IllegalStateException("FSS request failed: " + url) : last;
    }

    private String text(JsonNode node, String key) {
        if (node == null || node.isMissingNode() || node.get(key) == null || node.get(key).isNull()) {
            return "";
        }
        return sanitizeText(node.get(key).asText(""));
    }

    private String yearFromDate(String yyyymmdd) {
        if (yyyymmdd == null) {
            return "";
        }
        String v = yyyymmdd.trim();
        if (v.length() >= 4) {
            return v.substring(0, 4);
        }
        return "";
    }

    private String normalizeCorpName(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replaceAll("\\(\\s*주\\s*\\)", "")
                .replace("㈜", "")
                .replace("주식회사", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String collapseName(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[^0-9A-Za-z가-힣]", "").trim();
    }

    private String canonicalCorpName(String text) {
        if (text == null) {
            return "";
        }
        return collapseName(normalizeCorpName(text))
                .toLowerCase()
                .replace("에스케이", "sk")
                .replace("에이치엠엠", "hmm")
                .replace("hynix", "하이닉스")
                .replace("skc", "skc")
                .trim();
    }

    private List<String> buildSearchNames(String corpName) {
        List<String> candidates = new ArrayList<>();
        addCandidate(candidates, corpName);
        addCandidate(candidates, corpName.replaceFirst("^SK", "에스케이"));
        addCandidate(candidates, corpName.replaceFirst("^CJ", "씨제이"));
        addCandidate(candidates, corpName.replaceFirst("^LG", "엘지"));
        addCandidate(candidates, corpName.replaceFirst("^DB", "디비"));
        addCandidate(candidates, corpName.replaceFirst("^GS", "지에스"));
        addCandidate(candidates, corpName.replaceFirst("^HD", "에이치디"));
        addCandidate(candidates, corpName.replaceFirst("^NH", "엔에이치"));
        addCandidate(candidates, corpName.replaceFirst("^KCC$", "케이씨씨"));
        addCandidate(candidates, corpName.replaceFirst("^POSCO", "포스코"));
        addCandidate(candidates, corpName.replaceFirst("^LS\\s+ELECTRIC$", "엘에스일렉트릭"));
        addCandidate(candidates, corpName.replaceFirst("^LSELECTRIC$", "엘에스일렉트릭"));
        addCandidate(candidates, transliterateCorpName(corpName));
        return candidates;
    }

    private void addCandidate(List<String> candidates, String value) {
        String normalized = normalizeCorpName(value);
        if (!normalized.isBlank() && !candidates.contains(normalized)) {
            candidates.add(normalized);
        }
    }

    private boolean hasResolvedCorp(JsonNode node) {
        return node != null
                && !node.isMissingNode()
                && !text(node, "crno").isBlank();
    }

    private String transliterateCorpName(String corpName) {
        if (corpName == null || corpName.isBlank()) {
            return "";
        }
        String normalized = normalizeCorpName(corpName)
                .replace("E&A", "이앤에이")
                .replace("e&a", "이앤에이")
                .replace("&", "앤")
                .replace(".", " ")
                .replace("-", " ")
                .replaceAll("\\s+", " ")
                .trim();
        StringBuilder result = new StringBuilder();
        for (String token : normalized.split(" ")) {
            if (token.isBlank()) {
                continue;
            }
            String converted = transliterateMixedToken(token);
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(converted);
        }
        return result.toString().replace(" ", "");
    }

    private String transliterateMixedToken(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        Matcher matcher = LATIN_RUN_PATTERN.matcher(token);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(transliterateToken(matcher.group())));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String transliterateToken(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        String upper = token.toUpperCase();
        if (WORD_ALIAS.containsKey(upper)) {
            return WORD_ALIAS.get(upper);
        }
        if (!token.matches("[A-Za-z]+")) {
            return token;
        }
        StringBuilder converted = new StringBuilder();
        for (char ch : upper.toCharArray()) {
            String syllable = LETTER_TO_KOREAN.get(ch);
            converted.append(syllable == null ? String.valueOf(ch) : syllable);
        }
        return converted.toString();
    }

    private static Map<Character, String> createLetterToKoreanMap() {
        Map<Character, String> map = new HashMap<>();
        map.put('A', "에이");
        map.put('B', "비");
        map.put('C', "씨");
        map.put('D', "디");
        map.put('E', "이");
        map.put('F', "에프");
        map.put('G', "지");
        map.put('H', "에이치");
        map.put('I', "아이");
        map.put('J', "제이");
        map.put('K', "케이");
        map.put('L', "엘");
        map.put('M', "엠");
        map.put('N', "엔");
        map.put('O', "오");
        map.put('P', "피");
        map.put('Q', "큐");
        map.put('R', "알");
        map.put('S', "에스");
        map.put('T', "티");
        map.put('U', "유");
        map.put('V', "브이");
        map.put('W', "더블유");
        map.put('X', "엑스");
        map.put('Y', "와이");
        map.put('Z', "지");
        return map;
    }

    private static Map<String, String> createWordAliasMap() {
        Map<String, String> map = new HashMap<>();
        map.put("POSCO", "포스코");
        map.put("NAVER", "네이버");
        map.put("SOOP", "숲");
        map.put("ELECTRIC", "일렉트릭");
        map.put("ENT", "엔터테인먼트");
        map.put("QNC", "큐엔씨");
        map.put("IPS", "아이피에스");
        map.put("HPSP", "에이치피에스피");
        map.put("DX", "디엑스");
        map.put("HK", "에이치케이");
        map.put("QNC", "큐엔씨");
        map.put("QNC", "큐엔씨");
        return map;
    }

    private String normalizeIndustryGroup(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replaceFirst("^FICS\\s+", "")
                .trim();
    }

    private String sanitizeText(String text) {
        if (text == null) {
            return "";
        }
        return HtmlUtils.htmlUnescape(text)
                .replace("&cr;", " ")
                .replace("\r", " ")
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String fallbackOverview(
            String corpName,
            String mainBiz,
            String year,
            List<String> affiliates,
            List<String> subsidiaries
    ) {
        String biz = (mainBiz == null || mainBiz.isBlank()) ? "주요" : mainBiz;
        String est = (year == null || year.isBlank()) ? "설립연도 미상" : year + "년";
        String af = affiliates.isEmpty() ? "없음" : String.join(", ", affiliates);
        String sb = subsidiaries.isEmpty() ? "없음" : String.join(", ", subsidiaries);
        return "%s%s %s 설립된 기업으로 %s 사업을 영위하고 있다. 주요 계열사는 %s이며, 주요 종속 기업은 %s이다."
                .formatted(corpName, topicParticle(corpName), est, biz, af, sb);
    }

    private String topicParticle(String text) {
        if (text == null || text.isBlank()) {
            return "은";
        }
        char last = text.charAt(text.length() - 1);
        if (last < 0xAC00 || last > 0xD7A3) {
            return "은";
        }
        int jong = (last - 0xAC00) % 28;
        return jong == 0 ? "는" : "은";
    }
}
