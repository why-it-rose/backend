package com.whyitrose.domain.stock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "stocks",
        uniqueConstraints = @UniqueConstraint(name = "uk_stocks_ticker", columnNames = "ticker"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 6)
    private String ticker;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 10)
    private String market;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(length = 50)
    private String sector;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static Stock create(
            String ticker,
            String name,
            String market,
            Integer displayOrder,
            String sector,
            String logoUrl) {
        Stock s = new Stock();
        s.ticker = ticker;
        s.name = name;
        s.market = market;
        s.displayOrder = displayOrder;
        s.sector = sector;
        s.logoUrl = logoUrl;
        return s;
    }

    public void applyMaster(
            String name,
            String market,
            Integer displayOrder,
            String sector,
            String logoUrl) {
        this.name = name;
        this.market = market;
        if (displayOrder != null) {
            this.displayOrder = displayOrder;
        }
        this.sector = sector;
        this.logoUrl = logoUrl;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
