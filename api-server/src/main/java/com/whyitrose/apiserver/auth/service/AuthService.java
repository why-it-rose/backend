package com.whyitrose.apiserver.auth.service;

import com.whyitrose.apiserver.auth.dto.AuthResult;
import com.whyitrose.apiserver.auth.dto.LoginRequest;
import com.whyitrose.apiserver.auth.dto.LoginResponse;
import com.whyitrose.apiserver.auth.dto.SignupRequest;
import com.whyitrose.apiserver.auth.dto.UserResponse;
import com.whyitrose.apiserver.auth.exception.AuthErrorCode;
import com.whyitrose.apiserver.auth.jwt.JwtClaims;
import com.whyitrose.apiserver.auth.jwt.JwtTokenProvider;
import com.whyitrose.core.exception.BaseException;
import com.whyitrose.core.response.BaseResponseStatus;
import com.whyitrose.domain.common.Status;
import com.whyitrose.domain.user.AuthProvider;
import com.whyitrose.domain.user.RefreshToken;
import com.whyitrose.domain.user.RefreshTokenRepository;
import com.whyitrose.domain.user.User;
import com.whyitrose.domain.user.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.whyitrose.apiserver.auth.dto.UpdateMeRequest;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public UserResponse signup(SignupRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new BaseException(AuthErrorCode.AUTH_002);
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new BaseException(AuthErrorCode.AUTH_010);
        }

        User user = User.create(
                request.name(),
                email,
                passwordEncoder.encode(request.password()),
                request.nickname(),
                AuthProvider.EMAIL,
                null
        );

        return UserResponse.from(userRepository.save(user));
    }

    public AuthResult login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(AuthErrorCode.AUTH_011));

        if (user.getStatus() == Status.DELETED) {
            throw new BaseException(AuthErrorCode.AUTH_013);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BaseException(AuthErrorCode.AUTH_011);
        }

        AuthResult result = toAuthResult(user);
        upsertRefreshToken(user, result.refreshToken());
        return result;
    }

    // refresh 토큰은 DB 저장값과 일치 여부만 검증하고, access만 새 발급
    public AuthResult refresh(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BaseException(BaseResponseStatus.INVALID_TOKEN);
        }

        JwtClaims claims;
        try {
            claims = jwtTokenProvider.parseClaims(refreshToken);
        } catch (ExpiredJwtException e) {
            throw new BaseException(BaseResponseStatus.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BaseException(BaseResponseStatus.INVALID_TOKEN);
        }

        if (!"REFRESH".equals(claims.tokenType())) {
            throw new BaseException(BaseResponseStatus.NOT_REFRESH_TOKEN);
        }

        User user = userRepository.findById(claims.userId())
                .orElseThrow(() -> new BaseException(BaseResponseStatus.INVALID_TOKEN));

        if (user.getStatus() == Status.DELETED) {
            throw new BaseException(AuthErrorCode.AUTH_013);
        }

        RefreshToken saved = refreshTokenRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BaseException(BaseResponseStatus.INVALID_TOKEN));

        if (saved.isExpired(LocalDateTime.now())) {
            refreshTokenRepository.delete(saved);
            throw new BaseException(BaseResponseStatus.EXPIRED_TOKEN);
        }

        if (!saved.getRefreshToken().equals(refreshToken)) {
            throw new BaseException(BaseResponseStatus.INVALID_TOKEN);
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId());

        return new AuthResult(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                newAccessToken,
                saved.getRefreshToken()
        );
    }

    public void logout(Long authenticatedUserId, String refreshToken) {
        if (authenticatedUserId != null) {
            refreshTokenRepository.deleteByUserId(authenticatedUserId);
            return;
        }

        // access 없는 로그아웃도 허용하려면 refresh 쿠키로 삭제 시도
        if (!StringUtils.hasText(refreshToken)) {
            return;
        }

        try {
            JwtClaims claims = jwtTokenProvider.parseClaims(refreshToken);
            if ("REFRESH".equals(claims.tokenType())) {
                refreshTokenRepository.deleteByUserId(claims.userId());
            }
        } catch (JwtException | IllegalArgumentException ignored) {
            // 쿠키는 어차피 만료시킬 것이므로 무시
        }
    }

    @Transactional(readOnly = true)
    public LoginResponse getMe(Long authenticatedUserId) {
        if (authenticatedUserId == null) {
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED_ACCESS);
        }

        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.INVALID_TOKEN));

        if (user.getStatus() == Status.DELETED) {
            throw new BaseException(AuthErrorCode.AUTH_013);
        }

        return new LoginResponse(user.getId(), user.getEmail(), user.getNickname());
    }

    public UserResponse updateMe(Long authenticatedUserId, UpdateMeRequest request) {
        if (authenticatedUserId == null) {
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED_ACCESS);
        }

        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.INVALID_TOKEN));

        if (user.getStatus() == Status.DELETED) {
            throw new BaseException(AuthErrorCode.AUTH_013);
        }

        String nickname = request.nickname().trim();
        if (userRepository.existsByNicknameAndIdNot(nickname, user.getId())) {
            throw new BaseException(AuthErrorCode.AUTH_010);
        }

        user.updateNickname(nickname);
        return UserResponse.from(user);
    }

    public void deleteMe(Long authenticatedUserId) {
        if (authenticatedUserId == null) {
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED_ACCESS);
        }

        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.INVALID_TOKEN));

        if (user.getStatus() == Status.DELETED) {
            throw new BaseException(AuthErrorCode.AUTH_013);
        }

        user.delete();
        refreshTokenRepository.deleteByUserId(user.getId());
    }

    private void upsertRefreshToken(User user, String refreshTokenValue) {
        LocalDateTime expiryAt = LocalDateTime.now()
                .plusSeconds(jwtTokenProvider.getRefreshTokenExpirationMs() / 1000);

        RefreshToken saved = refreshTokenRepository.findByUserId(user.getId()).orElse(null);
        if (saved == null) {
            refreshTokenRepository.save(RefreshToken.create(user, refreshTokenValue, expiryAt));
            return;
        }

        saved.update(refreshTokenValue, expiryAt);
    }

    private AuthResult toAuthResult(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        return new AuthResult(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                accessToken,
                refreshToken
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
