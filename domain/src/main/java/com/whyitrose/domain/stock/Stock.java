package com.whyitrose.domain.stock;

import com.whyitrose.domain.common.BaseTimeEntity;
import com.whyitrose.domain.common.Status;
import com.whyitrose.domain.event.Event;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "stocks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_stocks_ticker", columnNames = {"ticker"})
        },
        indexes = {
                @Index(name = "idx_stocks_name", columnList = "name"),
                @Index(name = "idx_stocks_status", columnList = "status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // 종목코드 ex) 005930
    @Column(name = "ticker", length = 20, nullable = false)
    private String ticker;

    // 종목명 ex) 삼성전자
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "market", length = 20, nullable = false)
    private StockMarket market;

    @Column(name = "sector", length = 100)
    private String sector;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    // DEFAULT 'ACTIVE'
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status;

    @OneToMany(mappedBy = "stock", fetch = FetchType.LAZY)
    private List<Event> events = new ArrayList<>();

    public static Stock create(String ticker, String name, StockMarket market,
                               String sector, String logoUrl) {
        Stock stock = new Stock();
        stock.ticker = ticker;
        stock.name = name;
        stock.market = market;
        stock.sector = sector;
        stock.logoUrl = logoUrl;
        stock.status = Status.ACTIVE;
        return stock;
    }

    public void deactivate() {
        this.status = Status.INACTIVE;
    }

    public void delete() {
        this.status = Status.DELETED;
    }
}
