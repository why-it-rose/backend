package com.whyitrose.apiserver.me.controller;

import com.whyitrose.apiserver.me.dto.AddInterestStockRequest;
import com.whyitrose.apiserver.me.service.InterestStockService;
import com.whyitrose.apiserver.stock.dto.StockDtos.InterestStockListResponse;
import com.whyitrose.core.exception.BaseException;
import com.whyitrose.core.response.BaseResponse;
import com.whyitrose.core.response.BaseResponseStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me/interest-stocks")
public class MeInterestStockController {

    private final InterestStockService interestStockService;

    @GetMapping
    public ResponseEntity<BaseResponse<InterestStockListResponse>> list(Authentication authentication) {
        Long userId = requireUserId(authentication);
        return ResponseEntity.ok(BaseResponse.success(interestStockService.list(userId)));
    }

    @PostMapping
    public ResponseEntity<BaseResponse<Void>> add(
            Authentication authentication,
            @Valid @RequestBody AddInterestStockRequest request
    ) {
        Long userId = requireUserId(authentication);
        interestStockService.add(userId, request.stockId());
        return ResponseEntity.ok(BaseResponse.success(null));
    }

    @DeleteMapping("/{stockId}")
    public ResponseEntity<BaseResponse<Void>> remove(
            Authentication authentication,
            @PathVariable Long stockId
    ) {
        Long userId = requireUserId(authentication);
        interestStockService.remove(userId, stockId);
        return ResponseEntity.ok(BaseResponse.success(null));
    }

    private Long requireUserId(Authentication authentication) {
        Long userId = extractPrincipalUserId(authentication);
        if (userId == null) {
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED_ACCESS);
        }
        return userId;
    }

    private Long extractPrincipalUserId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long id) {
            return id;
        }
        if (principal instanceof Integer id) {
            return id.longValue();
        }
        return null;
    }
}
