package com.whyitrose.apiserver.auth.service;

import com.whyitrose.apiserver.auth.dto.SignupRequest;
import com.whyitrose.apiserver.auth.dto.UserResponse;
import com.whyitrose.apiserver.auth.exception.AuthErrorCode;
import com.whyitrose.core.exception.BaseException;
import com.whyitrose.domain.user.AuthProvider;
import com.whyitrose.domain.user.User;
import com.whyitrose.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}