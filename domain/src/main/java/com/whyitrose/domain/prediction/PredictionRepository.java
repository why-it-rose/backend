package com.whyitrose.domain.prediction;

import com.whyitrose.domain.common.Status;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    Optional<Prediction> findByUserIdAndDigestIdAndStockId(
            Long userId, Long digestId, Long stockId);

    // idx_predictions_user 활용
    List<Prediction> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 커서 기반 페이지네이션 (cursor 이하 id, 최신순)
    List<Prediction> findByUserIdAndIdLessThanOrderByIdDesc(Long userId, Long cursor, Pageable pageable);

    // 커서 없을 때 (첫 페이지)
    List<Prediction> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    // 통계용: 전체 예측 수
    long countByUserId(Long userId);

    // 통계용: 집계 완료된 예측
    List<Prediction> findByUserIdAndActualChangePctIsNotNull(Long userId);

    // 주간 통계용: 특정 기간 내 집계 완료된 예측
    List<Prediction> findByUserIdAndActualChangePctIsNotNullAndCreatedAtBetween(
            Long userId, LocalDateTime start, LocalDateTime end);

    // 주간 통계용: 특정 기간 내 전체 예측 수
    long countByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

    // 복기 미완료 — 배치용
    List<Prediction> findByActualChangePctIsNullAndStatus(Status status);
}