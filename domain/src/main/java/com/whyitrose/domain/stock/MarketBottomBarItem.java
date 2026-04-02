package com.whyitrose.domain.stock;

import com.whyitrose.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "market_bottom_bar_items",
        indexes = {
                @Index(name = "idx_market_bottom_bar_items_display_order", columnList = "display_order")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketBottomBarItem extends BaseTimeEntity {

    @Id
    @Column(name = "id", length = 50, nullable = false)
    private String id;

    @Column(name = "label", length = 100, nullable = false)
    private String label;

    @Column(name = "shcode", length = 20, nullable = false, unique = true)
    private String shcode;

    @Column(name = "info_url", length = 500, nullable = false)
    private String infoUrl;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "current_price", nullable = false)
    private Long currentPrice;

    @Column(name = "price_change", nullable = false)
    private Long priceChange;

    @Column(name = "change_rate", nullable = false)
    private Double changeRate;

    public static MarketBottomBarItem create(
            String id,
            String label,
            String shcode,
            String infoUrl,
            Integer displayOrder
    ) {
        MarketBottomBarItem item = new MarketBottomBarItem();
        item.id = id;
        item.label = label;
        item.shcode = shcode;
        item.infoUrl = infoUrl;
        item.displayOrder = displayOrder;
        item.currentPrice = 0L;
        item.priceChange = 0L;
        item.changeRate = 0.0;
        return item;
    }

    public void update(String label, String shcode, String infoUrl, Integer displayOrder) {
        this.label = label;
        this.shcode = shcode;
        this.infoUrl = infoUrl;
        this.displayOrder = displayOrder;
    }

    public void updateQuote(long currentPrice, long priceChange, double changeRate) {
        this.currentPrice = currentPrice;
        this.priceChange = priceChange;
        this.changeRate = changeRate;
    }
}
