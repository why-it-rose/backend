package com.whyitrose.batch.stock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whyitrose.batch.config.LsOpenApiProperties;
import com.whyitrose.batch.ls.LsMarketDataClient;
import com.whyitrose.domain.stock.Stock;
import com.whyitrose.domain.stock.StockMarket;
import com.whyitrose.domain.stock.StockRepository;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockMasterLoadService {

    private static final String LOGO_URL_TEMPLATE =
            "https://thumb.tossinvest.com/image/resized/96x0/https%%3A%%2F%%2Fstatic.toss.im%%2Fpng-icons%%2Fsecurities%%2Ficn-sec-fill-%s.png";

    private final LsMarketDataClient lsMarketDataClient;
    private final StockRepository stockRepository;
    private final LsOpenApiProperties lsOpenApiProperties;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Transactional
    public void loadAndUpsert() throws InterruptedException {
        String token = lsOpenApiProperties.getAccessToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "LS OpenAPI access token이 비어 있습니다. 환경변수 LS_ACCESS_TOKEN 또는 ls.openapi.access-token을 설정하세요.");
        }

        SeedContext seed = loadSeedContext();
        if (seed.active()) {
            log.info(
                    "시드 모드: KOSPI {} + KOSDAQ {} 티커로 t9945 결과 필터",
                    seed.kospiCount(),
                    seed.kosdaqCount());
        } else if (lsOpenApiProperties.isFullMasterWhenSeedEmpty()) {
            log.warn(
                    "시드가 비어 있거나 리소스가 없습니다. t9945 KOSPI/KOSDAQ 전량을 stocks에 적재합니다. "
                            + "이후 KOSPI200+KOSDAQ50만 쓰려면 seed/index-universe.json을 채우세요. (docs/250-stocks-storage-spec.md §2.6 참고)");
        } else {
            throw new IllegalStateException(
                    "시드 티커가 없고 full-master-when-seed-empty=false 입니다. 시드를 채우거나 해당 옵션을 켜세요.");
        }

        List<JsonNode> kospiRows = lsMarketDataClient.fetchAllMasterRows("1");
        Thread.sleep(Math.max(lsOpenApiProperties.getMinIntervalMs(), 500L));
        List<JsonNode> kosdaqRows = lsMarketDataClient.fetchAllMasterRows("2");

        List<ParsedRow> parsed = new ArrayList<>();
        parsed.addAll(parseRows(kospiRows, StockMarket.KOSPI));
        parsed.addAll(parseRows(kosdaqRows, StockMarket.KOSDAQ));

        List<ParsedRow> toSave = applySeedFilter(parsed, seed);

        log.info("적재 대상 종목 수: {} (t9945 원본 KOSPI {} + KOSDAQ {} 행)", toSave.size(), kospiRows.size(), kosdaqRows.size());

        if (seed.active()) {
            Set<String> savedTickers = toSave.stream().map(ParsedRow::ticker).collect(Collectors.toSet());
            for (String t : seed.displayOrder().keySet()) {
                if (!savedTickers.contains(t)) {
                    log.warn("시드 티커가 t9945 결과에 없습니다: {}", t);
                }
            }
        }

        for (ParsedRow row : toSave) {
            upsertOne(row);
        }
    }

    private void upsertOne(ParsedRow row) {
        if (stockRepository.existsByTicker(row.ticker())) {
            return;
        }
        stockRepository.save(Stock.create(
                row.ticker(),
                row.name(),
                row.market(),
                null,
                buildLogoUrl(row.ticker())));
    }

    private List<ParsedRow> applySeedFilter(List<ParsedRow> rows, SeedContext seed) {
        if (!seed.active()) {
            return rows;
        }
        Set<String> allow = seed.displayOrder().keySet();
        List<ParsedRow> out = new ArrayList<>();
        for (ParsedRow r : rows) {
            if (allow.contains(r.ticker())) {
                out.add(r);
            }
        }
        return out;
    }

    private List<ParsedRow> parseRows(List<JsonNode> nodes, StockMarket market) {
        List<ParsedRow> list = new ArrayList<>();
        for (JsonNode n : nodes) {
            String rawCode = text(n, "shcode");
            if (rawCode == null || rawCode.isBlank()) {
                continue;
            }
            String ticker = normalizeTicker(rawCode);
            String name = text(n, "hname");
            if (name == null || name.isBlank()) {
                log.debug("종목명 없음, 스킵: {}", ticker);
                continue;
            }
            list.add(new ParsedRow(
                    ticker,
                    name.trim(),
                    market));
        }
        return list;
    }

    private SeedContext loadSeedContext() {
        String loc = lsOpenApiProperties.getSeedResource();
        if (loc == null || loc.isBlank()) {
            return SeedContext.empty();
        }
        Resource resource = resourceLoader.getResource(loc);
        if (!resource.exists()) {
            log.warn("시드 리소스 없음: {}", loc);
            return SeedContext.empty();
        }
        try (InputStream in = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(in);
            List<String> kospi = readTickerArray(root.get("kospi200"));
            List<String> kosdaq = readTickerArray(root.get("kosdaq50"));
            LinkedHashMap<String, Integer> order = new LinkedHashMap<>();
            int i = 1;
            for (String t : kospi) {
                order.put(normalizeTicker(t), i++);
            }
            for (String t : kosdaq) {
                order.put(normalizeTicker(t), i++);
            }
            return new SeedContext(!order.isEmpty(), order, kospi.size(), kosdaq.size());
        } catch (Exception e) {
            throw new IllegalStateException("시드 파일을 읽을 수 없습니다: " + loc, e);
        }
    }

    private static List<String> readTickerArray(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return list;
        }
        for (JsonNode el : node) {
            if (el.isTextual()) {
                list.add(el.asText());
            }
        }
        return list;
    }

    private static String text(JsonNode n, String field) {
        if (n == null || !n.has(field) || n.get(field).isNull()) {
            return null;
        }
        String s = n.get(field).asText();
        return s == null ? null : s.trim();
    }

    private static String normalizeTicker(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (value.isEmpty()) {
            return value;
        }
        // 숫자 6자리 티커는 0-padding, 영문 포함 티커는 원본 그대로 사용
        if (value.matches("\\d+")) {
            return String.format("%6s", value).replace(' ', '0');
        }
        return value;
    }

    private static String buildLogoUrl(String ticker) {
        return String.format(LOGO_URL_TEMPLATE, ticker);
    }

    private record ParsedRow(
            String ticker,
            String name,
            StockMarket market) {}

    private record SeedContext(boolean active, Map<String, Integer> displayOrder, int kospiCount, int kosdaqCount) {

        static SeedContext empty() {
            return new SeedContext(false, Map.of(), 0, 0);
        }
    }
}
