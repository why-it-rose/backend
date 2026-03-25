package com.whyitrose.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
// findByUserId와 deleteByUserId를 통해 userId의 refresh token 조회 및 삭제
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
