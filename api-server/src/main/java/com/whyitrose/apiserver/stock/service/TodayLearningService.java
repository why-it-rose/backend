package com.whyitrose.apiserver.stock.service;

import com.whyitrose.apiserver.stock.dto.TodayLearningDtos.LearningNewsItem;
import com.whyitrose.apiserver.stock.dto.TodayLearningDtos.LearningPinResponse;
import com.whyitrose.apiserver.stock.dto.TodayLearningDtos.PredictionInfo;
import com.whyitrose.apiserver.stock.dto.TodayLearningDtos.TodayLearningDetailResponse;
import com.whyitrose.apiserver.stock.exception.StockErrorCode;
import com.whyitrose.core.exception.BaseException;
import com.whyitrose.domain.common.Status;
import com.whyitrose.domain.digest.DailyNewsDigestItem;
import com.whyitrose.domain.digest.DailyNewsDigestItemRepository;
import com.whyitrose.domain.news.NewsTagRepository;
import com.whyitrose.domain.notification.NotificationRepository;
import com.whyitrose.domain.prediction.PredictionRepository;
import com.whyitrose.domain.stock.StockPrice;
import com.whyitrose.domain.stock.StockPricePeriod;
import com.whyitrose.domain.stock.StockPriceRepository;
import com.whyitrose.domain.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.whyitrose.apiserver.stock.dto.TodayLearningDtos.formatDigestDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodayLearningService {

    private final StockRepository stockRepository;
    private final StockPriceRepository stockPriceRepository;
    private final DailyNewsDigestItemRepository digestItemRepository;
    private final NewsTagRepository newsTagRepository;
    private final NotificationRepository notificationRepository;
    private final PredictionRepository predictionRepository;

    public Optional<LearningPinResponse> getLearningPin(Long stockId) {
        stockRepository.findById(stockId)
                .orElseThrow(() -> new BaseException(StockErrorCode.STOCK_001));

        Optional<DailyNewsDigestItem> latestOpt = resolveLatestDigestItem(stockId);
        if (latestOpt.isEmpty()) {
            return Optional.empty();
        }

        DailyNewsDigestItem latest = latestOpt.get();
        LocalDate digestDate = latest.getDigest().getDigestDate();
        Long digestId = latest.getDigest().getId();

        int newsCount = digestItemRepository
                .findByDigestIdAndStockIdAndStatus(digestId, stockId, Status.ACTIVE)
                .size();

        return Optional.of(LearningPinResponse.of(digestDate, newsCount));
    }

    public Optional<TodayLearningDetailResponse> getTodayLearning(Long stockId) {
        var stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new BaseException(StockErrorCode.STOCK_001));

        Optional<DailyNewsDigestItem> latestOpt = resolveLatestDigestItem(stockId);
        if (latestOpt.isEmpty()) {
            return Optional.empty();
        }

        DailyNewsDigestItem latest = latestOpt.get();
        LocalDate digestDate = latest.getDigest().getDigestDate();
        Long digestId = latest.getDigest().getId();

        // 가격 정보 — 전날(digestDate) 종가 + 전전날 종가
        // findRecentPricesBeforeDate: tradingDate < targetDate 조건이므로
        // digestDate + 1을 넘기면 digestDate 이하 최근 2건 반환
        List<StockPrice> recentPrices = stockPriceRepository
                .findRecentPricesBeforeDate(stockId, digestDate.plusDays(1), PageRequest.of(0, 2));

        StockPrice currentPrice = recentPrices.size() >= 1 ? recentPrices.get(0) : null;
        StockPrice prevPrice    = recentPrices.size() >= 2 ? recentPrices.get(1) : null;

        // 전날 종가가 정확히 digestDate 데이터인지 확인 (공휴일 등 누락 방어)
        boolean hasCurrentPrice = currentPrice != null
                && currentPrice.getTradingDate().equals(digestDate);
        Integer priceClose     = hasCurrentPrice ? currentPrice.getClosePrice() : null;
        Integer prevPriceClose = (hasCurrentPrice && prevPrice != null)
                ? prevPrice.getClosePrice() : null;
        String changeRate      = formatChangeRate(priceClose, prevPriceClose);

        // 뉴스 목록 — news JOIN FETCH로 N+1 방지
        List<DailyNewsDigestItem> items = digestItemRepository
                .findByDigestIdAndStockIdWithNews(digestId, stockId);

        List<Long> newsIds = items.stream()
                .map(i -> i.getNews().getId())
                .toList();

        // 태그 배치 조회 — newsId IN 절로 N+1 방지
        Map<Long, List<String>> tagsByNewsId = newsIds.isEmpty()
                ? Map.of()
                : newsTagRepository.findByNewsIdInWithTag(newsIds).stream()
                        .collect(Collectors.groupingBy(
                                nt -> nt.getNews().getId(),
                                Collectors.mapping(nt -> nt.getTag().getName(), Collectors.toList())
                        ));

        List<LearningNewsItem> newsList = items.stream()
                .map(i -> LearningNewsItem.from(
                        i.getNews(),
                        tagsByNewsId.getOrDefault(i.getNews().getId(), List.of())
                ))
                .toList();

        // 예측 상태 — 선택적 인증 (비로그인·미예측 시 null → @JsonInclude(NON_NULL)으로 응답에서 생략)
        PredictionInfo predictionInfo = resolvePrediction(stockId, digestId);

        return Optional.of(new TodayLearningDetailResponse(
                formatDigestDate(digestDate),
                stock.getName(),
                changeRate,
                priceClose,
                prevPriceClose,
                predictionInfo,
                newsList
        ));
    }

    // ──────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────

    /** 두 엔드포인트 공통 — 해당 종목의 가장 최신 digest item */
    private Optional<DailyNewsDigestItem> resolveLatestDigestItem(Long stockId) {
        List<DailyNewsDigestItem> results = digestItemRepository
                .findLatestByStockId(stockId, PageRequest.of(0, 1));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * 예측 상태 조회
     * - 비로그인 또는 미예측: null (응답에서 prediction 필드 생략)
     * - 로그인 + 예측 완료: PredictionInfo.from(prediction)
     */
    private PredictionInfo resolvePrediction(Long stockId, Long digestId) {
        Long userId = resolveCurrentUserId();
        if (userId == null) {
            return null;
        }

        return notificationRepository
                .findByUserIdAndDigestIdAndStatus(userId, digestId, Status.ACTIVE)
                .flatMap(notification -> predictionRepository
                        .findByUserIdAndNotificationIdAndStockId(userId, notification.getId(), stockId))
                .map(PredictionInfo::from)
                .orElse(null);
    }

    /** SecurityContext에서 userId 추출 — 비로그인 시 null */
    private Long resolveCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof UsernamePasswordAuthenticationToken
                && auth.getPrincipal() instanceof Long userId) {
            return userId;
        }
        return null;
    }

    /**
     * 전전날 종가 대비 전날 종가 등락률 (+1.23% / -2.08%)
     * 둘 중 하나라도 없으면 null → @JsonInclude(NON_NULL)으로 응답에서 생략
     */
    private String formatChangeRate(Integer priceClose, Integer prevPriceClose) {
        if (priceClose == null || prevPriceClose == null || prevPriceClose == 0) {
            return null;
        }
        double rate = ((double) (priceClose - prevPriceClose) / prevPriceClose) * 100.0;
        return String.format("%+.2f%%", rate);
    }
}