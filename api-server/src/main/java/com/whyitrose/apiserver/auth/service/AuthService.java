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

    //이메일 정규화
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}