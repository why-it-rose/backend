package com.whyitrose.apiserver.prediction.service;

import com.whyitrose.apiserver.me.dto.MyPageStatsResponse;
import com.whyitrose.apiserver.me.dto.WeeklySummaryResponse;
import com.whyitrose.apiserver.prediction.dto.PredictionGroupResponse;
import com.whyitrose.apiserver.prediction.dto.PredictionPageResponse;
import com.whyitrose.apiserver.prediction.dto.PredictionRequest;
import com.whyitrose.apiserver.prediction.dto.PredictionResponse;
import com.whyitrose.apiserver.prediction.dto.PredictionStatusResponse;
import com.whyitrose.apiserver.prediction.exception.PredictionErrorCode;
import com.whyitrose.core.exception.BaseException;
import com.whyitrose.domain.common.Status;
import com.whyitrose.domain.digest.DailyNewsDigest;
import com.whyitrose.domain.digest.DailyNewsDigestRepository;
import com.whyitrose.domain.prediction.Prediction;
import com.whyitrose.domain.prediction.PredictionDirection;
import com.whyitrose.domain.prediction.PredictionRepository;
import com.whyitrose.domain.scrap.ScrapRepository;
import com.whyitrose.domain.stock.Stock;
import com.whyitrose.domain.stock.StockRepository;
import com.whyitrose.domain.user.User;
import com.whyitrose.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PredictionService {

    private final PredictionRepository predictionRepository;
    private final DailyNewsDigestRepository dailyNewsDigestRepository;
    private final StockRepository stockRepository;
    private final UserRepository userRepository;
    private final ScrapRepository scrapRepository;

    @Transactional
    public PredictionResponse upsert(Long userId, PredictionRequest request) {
        DailyNewsDigest digest = dailyNewsDigestRepository.findById(request.digestId())
                .orElseThrow(() -> new BaseException(PredictionErrorCode.DIGEST_NOT_FOUND));

        if (!digest.getDigestDate().equals(LocalDate.now())) {
            throw new BaseException(PredictionErrorCode.PREDICTION_DATE_INVALID);
        }

        Stock stock = stockRepository.findById(request.stockId())
                .orElseThrow(() -> new BaseException(PredictionErrorCode.STOCK_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(PredictionErrorCode.USER_NOT_FOUND));

        Prediction prediction = predictionRepository
                .findByUserIdAndDigestIdAndStockId(userId, request.digestId(), request.stockId())
                .orElse(Prediction.create(user, digest, stock));

        prediction.updatePrediction(request.direction(), request.reason());
        predictionRepository.save(prediction);

        return PredictionResponse.from(prediction);
    }

    public List<PredictionResponse> getMyPredictions(Long userId) {
        return predictionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(PredictionResponse::from)
                .toList();
    }

    public PredictionPageResponse getMyPredictionsPaged(Long userId, Long cursor, int size) {
        PageRequest pageable = PageRequest.of(0, size + 1);

        List<Prediction> predictions = (cursor == null)
                ? predictionRepository.findByUserIdOrderByIdDesc(userId, pageable)
                : predictionRepository.findByUserIdAndIdLessThanOrderByIdDesc(userId, cursor, pageable);

        boolean hasNext = predictions.size() > size;
        List<Prediction> content = hasNext ? predictions.subList(0, size) : predictions;

        Long nextCursor = hasNext ? content.get(content.size() - 1).getId() : null;

        Map<LocalDate, List<PredictionResponse>> grouped = content.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getDigest().getDigestDate(),
                        LinkedHashMap::new,
                        Collectors.mapping(PredictionResponse::from, Collectors.toList())
                ));

        List<PredictionGroupResponse> groups = grouped.entrySet().stream()
                .map(e -> PredictionGroupResponse.of(e.getKey(), e.getValue()))
                .toList();

        return new PredictionPageResponse(groups, nextCursor, hasNext);
    }

    public MyPageStatsResponse getMyStats(Long userId) {
        long totalPredictions = predictionRepository.countByUserId(userId);

        List<Prediction> resolved = predictionRepository.findByUserIdAndActualChangePctIsNotNull(userId);

        long correctPredictions = resolved.stream()
                .filter(p -> Boolean.TRUE.equals(isCorrect(p)))
                .count();

        Double accuracy = resolved.isEmpty()
                ? null
                : Math.round((double) correctPredictions / resolved.size() * 1000) / 10.0;

        long totalScraps = scrapRepository.countByUserIdAndStatus(userId, Status.ACTIVE);

        return new MyPageStatsResponse(totalPredictions, correctPredictions, accuracy, totalScraps);
    }

    public WeeklySummaryResponse getWeeklySummary(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime end = today.with(DayOfWeek.SUNDAY).atTime(LocalTime.MAX);

        long weeklyTotal = predictionRepository.countByUserIdAndCreatedAtBetween(userId, start, end);

        List<Prediction> weeklyResolved = predictionRepository
                .findByUserIdAndActualChangePctIsNotNullAndCreatedAtBetween(userId, start, end);

        long weeklyCorrect = weeklyResolved.stream()
                .filter(p -> Boolean.TRUE.equals(isCorrect(p)))
                .count();

        Double weeklyAccuracy = weeklyTotal == 0
                ? null
                : Math.round((double) weeklyCorrect / weeklyTotal * 1000) / 10.0;

        return new WeeklySummaryResponse(weeklyTotal, weeklyCorrect, weeklyAccuracy);
    }

    private Boolean isCorrect(Prediction p) {
        if (p.getActualChangePct() == null) return null;
        return switch (p.getDirection()) {
            case UP -> p.getActualChangePct().compareTo(BigDecimal.ZERO) > 0;
            case DOWN -> p.getActualChangePct().compareTo(BigDecimal.ZERO) < 0;
            case SIDEWAYS -> p.getActualChangePct().abs().compareTo(new BigDecimal("2.00")) <= 0;
        };
    }

    public PredictionStatusResponse getStatus(Long userId, Long digestId, Long stockId) {
        DailyNewsDigest digest = dailyNewsDigestRepository.findById(digestId)
                .orElseThrow(() -> new BaseException(PredictionErrorCode.DIGEST_NOT_FOUND));

        boolean canPredict = digest.getDigestDate().equals(LocalDate.now());

        return predictionRepository
                .findByUserIdAndDigestIdAndStockId(userId, digestId, stockId)
                .map(p -> new PredictionStatusResponse(p.getId(), p.getDirection(), p.getReason(), canPredict))
                .orElse(new PredictionStatusResponse(null, null, null, canPredict));
    }
}
