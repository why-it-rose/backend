package com.whyitrose.domain.news;

import com.whyitrose.domain.common.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NewsStockRepository extends JpaRepository<NewsStock, Long> {

    Optional<NewsStock> findByNewsIdAndStockId(Long newsId, Long stockId);

    List<NewsStock> findByStockIdAndStatus(Long stockId, Status status);

    @Query("SELECT ns FROM NewsStock ns " +
            "JOIN FETCH ns.news n " +
            "JOIN FETCH ns.stock s " +
            "WHERE n.publishedAt >= :startOfDay " +
            "AND n.publishedAt <= :endOfDay " +
            "AND n.status = 'ACTIVE' " +
            "AND s.status = 'ACTIVE'")
    List<NewsStock> findAllWithNewsByPublishedAtBetween(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
}