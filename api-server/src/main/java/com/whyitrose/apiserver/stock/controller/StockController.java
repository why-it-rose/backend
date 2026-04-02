package com.whyitrose.apiserver.stock.controller;

import com.whyitrose.apiserver.stock.dto.StockDtos.StockDetailResponse;
import com.whyitrose.apiserver.stock.dto.StockDtos.StockCompanyResponse;
import com.whyitrose.apiserver.stock.dto.StockDtos.StockListResponse;
import com.whyitrose.apiserver.stock.dto.StockDtos.StockPricesResponse;
import com.whyitrose.apiserver.stock.dto.StockDtos.StockSearchResponse;
import com.whyitrose.apiserver.stock.service.StockService;
import com.whyitrose.core.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stocks")
public class StockController {

    private final StockService stockService;

    @GetMapping
    public ResponseEntity<BaseResponse<StockListResponse>> getStocks(
            @RequestParam(defaultValue = "ALL") String market,
            @RequestParam(defaultValue = "TRADING_AMOUNT") String sort,
            @RequestParam(defaultValue = "1D") String period,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                stockService.getStocks(market, sort, period, cursor, size)));
    }

    @GetMapping("/search")
    public ResponseEntity<BaseResponse<StockSearchResponse>> search(
            @RequestParam("q") String q,
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        return ResponseEntity.ok(BaseResponse.success(stockService.searchStocks(q, limit)));
    }

    @GetMapping("/{stockId}")
    public ResponseEntity<BaseResponse<StockDetailResponse>> getStockDetail(@PathVariable Long stockId) {
        return ResponseEntity.ok(BaseResponse.success(stockService.getStockDetail(stockId)));
    }

    @GetMapping("/{stockId}/prices")
    public ResponseEntity<BaseResponse<StockPricesResponse>> getStockPrices(
            @PathVariable Long stockId,
            @RequestParam(defaultValue = "6M") String period
    ) {
        return ResponseEntity.ok(BaseResponse.success(stockService.getStockPrices(stockId, period)));
    }

    @GetMapping("/{stockId}/company")
    public ResponseEntity<BaseResponse<StockCompanyResponse>> getStockCompany(@PathVariable Long stockId) {
        return ResponseEntity.ok(BaseResponse.success(stockService.getStockCompany(stockId)));
    }
}
