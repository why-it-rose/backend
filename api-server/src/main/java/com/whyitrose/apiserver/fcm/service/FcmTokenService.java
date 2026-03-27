package com.whyitrose.apiserver.fcm.service;

import com.whyitrose.apiserver.fcm.dto.FcmTokenRequest;
import com.whyitrose.core.exception.BaseException;
import com.whyitrose.core.response.BaseResponseStatus;
import com.whyitrose.domain.fcm.FcmToken;
import com.whyitrose.domain.fcm.FcmTokenRepository;
import com.whyitrose.domain.user.User;
import com.whyitrose.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FcmTokenService {
    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    public void upsertToken(Long userId, FcmTokenRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.INVALID_TOKEN));

        fcmTokenRepository.findByToken(request.token())
                .ifPresentOrElse(
                        existing -> existing.updateUser(user),
                        () -> fcmTokenRepository.save(FcmToken.create(user, request.token()))
                );
    }
}