package com.whyitrose.domain.stock;

import com.whyitrose.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "stock_list_snapshots",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_stock_list_snapshots", columnNames = {"stock_id", "period_key"})
        },
        indexes = {
                @Index(name = "idx_snapshot_amount", columnList = "period_key, trading_amount, stock_id"),
                @Index(name = "idx_snapshot_volume", columnList = "period_key, trading_volume, stock_id"),
                @Index(name = "idx_snapshot_change_rate", columnList = "period_key, change_rate, stock_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockListSnapshot extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(name = "period_key", length = 20, nullable = false)
    private String periodKey;

    @Column(name = "current_price", nullable = false)
    private long currentPrice;

    @Column(name = "price_change", nullable = false)
    private long priceChange;

    @Column(name = "change_rate", nullable = false)
    private double changeRate;

    @Column(name = "trading_amount", nullable = false)
    private long tradingAmount;

    @Column(name = "trading_volume", nullable = false)
    private long tradingVolume;

    public static StockListSnapshot create(
            Stock stock,
            String periodKey,
            long currentPrice,
            long priceChange,
            double changeRate,
            long tradingAmount,
            long tradingVolume
    ) {
        StockListSnapshot snapshot = new StockListSnapshot();
        snapshot.stock = stock;
        snapshot.periodKey = periodKey;
        snapshot.currentPrice = currentPrice;
        snapshot.priceChange = priceChange;
        snapshot.changeRate = changeRate;
        snapshot.tradingAmount = tradingAmount;
        snapshot.tradingVolume = tradingVolume;
        return snapshot;
    }

    public void apply(
            long currentPrice,
            long priceChange,
            double changeRate,
            long tradingAmount,
            long tradingVolume
    ) {
        this.currentPrice = currentPrice;
        this.priceChange = priceChange;
        this.changeRate = changeRate;
        this.tradingAmount = tradingAmount;
        this.tradingVolume = tradingVolume;
    }
}
