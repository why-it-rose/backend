package com.whyitrose.domain.prediction;

import com.whyitrose.domain.common.Status;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    Optional<Prediction> findByUserIdAndNotificationIdAndStockId(
            Long userId, Long notificationId, Long stockId);

    // idx_predictions_user 활용
    List<Prediction> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Status status, Pageable pageable);

    // 복기 미완료 — 배치용
    List<Prediction> findByActualChangePctIsNullAndStatus(Status status);
}