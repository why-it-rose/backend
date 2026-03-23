package com.whyitrose.apiserver.auth.service;

import com.whyitrose.apiserver.auth.dto.LoginRequest;
import com.whyitrose.apiserver.auth.dto.LoginResponse;
import com.whyitrose.apiserver.auth.dto.SignupRequest;
import com.whyitrose.apiserver.auth.dto.UserResponse;
import com.whyitrose.apiserver.auth.exception.AuthErrorCode;
import com.whyitrose.core.exception.BaseException;
import com.whyitrose.domain.common.Status;
import com.whyitrose.domain.user.AuthProvider;
import com.whyitrose.domain.user.User;
import com.whyitrose.domain.user.UserRepository;
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

    public UserResponse signup(SignupRequest request) {
        String email = normalizeEmail(request.email());
        //이메일 중복 체크
        if (userRepository.existsByEmail(email)) {
            throw new BaseException(AuthErrorCode.AUTH_002);
        }
        //닉네임 중복체크
        if (userRepository.existsByNickname(request.nickname())) {
            throw new BaseException(AuthErrorCode.AUTH_010);
        }
        //유저 생성
        User user = User.create(
                request.name(),
                email,
                passwordEncoder.encode(request.password()),
                request.nickname(),
                AuthProvider.EMAIL, //이메일 가입자
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

        return new LoginResponse(user.getId(), user.getEmail(), user.getNickname());
    }

    //외부 인증 결과를 가지고 우리 회원 정책에 맞게 로그인/가입을 결정
    public LoginResponse loginOrRegisterSocial(AuthProvider provider, String providerUid, String email, String name) {
        String normalizedEmail = normalizeEmail(email);
        //먼저 provider + providerUid로 조회
        User byProvider = userRepository.findByProviderAndProviderUid(provider, providerUid).orElse(null);
        if (byProvider != null) {
            if (byProvider.getStatus() == Status.DELETED) {
                throw new BaseException(AuthErrorCode.AUTH_013);
            }
            return new LoginResponse(byProvider.getId(), byProvider.getEmail(), byProvider.getNickname());
        }
        //없으면 email로 조회
        User byEmail = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (byEmail != null) {
            throw new BaseException(AuthErrorCode.AUTH_015);
        }

        String resolvedName = (name == null || name.isBlank()) ? "user" : name.trim();
        String nickname = resolveUniqueNickname(resolvedName);
        //새 가입자 가입 처리
        User created = User.create(
                resolvedName,
                normalizedEmail,
                null,
                nickname,
                provider,
                providerUid
        );

        User saved = userRepository.save(created);
        return new LoginResponse(saved.getId(), saved.getEmail(), saved.getNickname());
    }

    private String resolveUniqueNickname(String base) {
        String candidate = base;
        int seq = 1;
        while (userRepository.existsByNickname(candidate)) {
            candidate = base + seq++;
        }
        return candidate;
    }

    //이메일 정규화
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
