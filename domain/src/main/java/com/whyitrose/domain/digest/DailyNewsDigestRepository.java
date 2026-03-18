package com.whyitrose.domain.digest;

import com.whyitrose.domain.common.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyNewsDigestRepository extends JpaRepository<DailyNewsDigest, Long> {

    Optional<DailyNewsDigest> findByDigestDate(LocalDate digestDate);

    Optional<DailyNewsDigest> findByDigestDateAndStatus(LocalDate digestDate, Status status);
}