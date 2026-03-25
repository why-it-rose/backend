package com.whyitrose.apiserver.auth.service;

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
import com.whyitrose.domain.user.User;
import com.whyitrose.domain.user.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository userRepository;
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

    public LoginResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(AuthErrorCode.AUTH_011));

        if (user.getStatus() == Status.DELETED) {
            throw new BaseException(AuthErrorCode.AUTH_013);
        }

        if (user.getProvider() != AuthProvider.EMAIL) {
            throw new BaseException(AuthErrorCode.AUTH_012);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BaseException(AuthErrorCode.AUTH_011);
        }

        return toLoginResponse(user);
    }

    public LoginResponse loginOrRegisterSocial(AuthProvider provider, String providerUid, String email, String name) {
        String normalizedEmail = normalizeEmail(email);

        User byProvider = userRepository.findByProviderAndProviderUid(provider, providerUid).orElse(null);
        if (byProvider != null) {
            if (byProvider.getStatus() == Status.DELETED) {
                throw new BaseException(AuthErrorCode.AUTH_013);
            }
            return toLoginResponse(byProvider);
        }

        User byEmail = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (byEmail != null) {
            throw new BaseException(AuthErrorCode.AUTH_015);
        }

        String resolvedName = (name == null || name.isBlank()) ? "user" : name.trim();
        String nickname = resolveUniqueNickname(resolvedName);

        User created = User.create(
                resolvedName,
                normalizedEmail,
                null,
                nickname,
                provider,
                providerUid
        );

        User saved = userRepository.save(created);
        return toLoginResponse(saved);
    }

    public LoginResponse refresh(String refreshToken) {
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

        return toLoginResponse(user);
    }

    private LoginResponse toLoginResponse(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                accessToken,
                refreshToken
        );
    }

    private String resolveUniqueNickname(String base) {
        String candidate = base;
        int seq = 1;
        while (userRepository.existsByNickname(candidate)) {
            candidate = base + seq++;
        }
        return candidate;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
