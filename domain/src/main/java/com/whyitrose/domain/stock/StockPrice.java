package com.whyitrose.domain.stock;

import com.whyitrose.domain.common.BaseTimeEntity;
import com.whyitrose.domain.common.Status;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "stock_prices",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_stock_prices", columnNames = {"stock_id", "trading_date", "period"})
        },
        indexes = {
                @Index(name = "idx_stock_prices_date", columnList = "stock_id, period, trading_date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockPrice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    // 거래일
    @Column(name = "trading_date", nullable = false)
    private LocalDate tradingDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "period", length = 20, nullable = false)
    private StockPricePeriod period;

    // 시가
    @Column(name = "open_price", nullable = false)
    private int openPrice;

    // 종가
    @Column(name = "close_price", nullable = false)
    private int closePrice;

    // 고가
    @Column(name = "high_price", nullable = false)
    private int highPrice;

    // 저가
    @Column(name = "low_price", nullable = false)
    private int lowPrice;

    // 거래량
    @Column(name = "volume", nullable = false)
    private long volume;

    // DEFAULT 'ACTIVE'
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status;

    public static StockPrice create(Stock stock, LocalDate tradingDate,
                                    StockPricePeriod period,
                                    int openPrice, int closePrice,
                                    int highPrice, int lowPrice, long volume) {
        StockPrice stockPrice = new StockPrice();
        stockPrice.stock = stock;
        stockPrice.tradingDate = tradingDate;
        stockPrice.period = period;
        stockPrice.openPrice = openPrice;
        stockPrice.closePrice = closePrice;
        stockPrice.highPrice = highPrice;
        stockPrice.lowPrice = lowPrice;
        stockPrice.volume = volume;
        stockPrice.status = Status.ACTIVE;
        return stockPrice;
    }

    public void applyPrice(int openPrice, int closePrice, int highPrice, int lowPrice, long volume) {
        this.openPrice = openPrice;
        this.closePrice = closePrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.volume = volume;
        this.status = Status.ACTIVE;
    }

    public void delete() {
        this.status = Status.DELETED;
    }
}