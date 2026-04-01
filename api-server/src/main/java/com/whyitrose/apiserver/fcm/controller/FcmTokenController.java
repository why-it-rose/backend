package com.whyitrose.apiserver.fcm.controller;

import com.whyitrose.apiserver.fcm.dto.FcmTokenRequest;
import com.whyitrose.apiserver.fcm.service.FcmTokenService;
import com.whyitrose.core.response.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fcm")
public class FcmTokenController {
    private final FcmTokenService fcmTokenService;

    @PostMapping("/token")
    public ResponseEntity<BaseResponse<Void>> registerToken(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid FcmTokenRequest request
    ) {
        fcmTokenService.upsertToken(userId, request);
        return ResponseEntity.ok(BaseResponse.success(null));
    }
}