package com.whyitrose.apiserver.me.service;

import com.whyitrose.apiserver.stock.dto.StockDtos.InterestStockListResponse;
import com.whyitrose.apiserver.stock.dto.StockDtos.StockSearchItem;
import com.whyitrose.apiserver.stock.exception.StockErrorCode;
import com.whyitrose.apiserver.stock.service.StockService;
import com.whyitrose.core.exception.BaseException;
import com.whyitrose.core.response.BaseResponseStatus;
import com.whyitrose.domain.common.Status;
import com.whyitrose.domain.interest.InterestStock;
import com.whyitrose.domain.interest.InterestStockRepository;
import com.whyitrose.domain.stock.Stock;
import com.whyitrose.domain.stock.StockRepository;
import com.whyitrose.domain.user.User;
import com.whyitrose.domain.user.UserRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterestStockService {

    private final InterestStockRepository interestStockRepository;
    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final StockService stockService;

    @Transactional(readOnly = true)
    public InterestStockListResponse list(Long userId) {
        List<InterestStock> rows = interestStockRepository.findActiveByUserIdWithStock(userId, Status.ACTIVE);
        List<StockSearchItem> items = rows.stream()
                .map(InterestStock::getStock)
                .map(stockService::toSearchItem)
                .toList();
        return new InterestStockListResponse(items);
    }

    @Transactional
    public void add(Long userId, Long stockId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.UNAUTHORIZED_ACCESS));
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new BaseException(StockErrorCode.STOCK_001));
        if (stock.getStatus() != Status.ACTIVE) {
            throw new BaseException(StockErrorCode.STOCK_001);
        }
        Optional<InterestStock> existing = interestStockRepository.findByUserIdAndStockId(userId, stockId);
        if (existing.isEmpty()) {
            interestStockRepository.save(InterestStock.create(user, stock));
            return;
        }
        InterestStock row = existing.get();
        if (row.getStatus() == Status.DELETED) {
            row.reactivate();
        }
    }

    @Transactional
    public void remove(Long userId, Long stockId) {
        interestStockRepository.findByUserIdAndStockId(userId, stockId)
                .filter(is -> is.getStatus() == Status.ACTIVE)
                .ifPresent(InterestStock::delete);
    }
}
