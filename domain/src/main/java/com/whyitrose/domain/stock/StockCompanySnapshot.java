package com.whyitrose.domain.stock;

import com.whyitrose.domain.common.BaseTimeEntity;
import java.time.LocalDateTime;
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
        name = "stock_company_profiles",
        uniqueConstraints = {
                @UniqueConstraint(name = "stock_id", columnNames = {"stock_id"})
        },
        indexes = {
                @Index(name = "last_refreshed_at", columnList = "last_refreshed_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockCompanySnapshot extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(name = "industry_group", length = 200)
    private String industryGroup;

    @Column(name = "sub_industry", length = 200)
    private String subIndustry;

    @Column(name = "sector_tags_json", columnDefinition = "text")
    private String sectorTagsJson;

    @Column(name = "market_cap")
    private Long marketCap;

    @Column(name = "market_rank")
    private Integer marketRank;

    @Column(name = "total_shares")
    private Long totalShares;

    @Column(name = "foreign_ratio")
    private Double foreignRatio;

    @Column(name = "overview", columnDefinition = "text")
    private String overview;

    @Column(name = "financials_base_date", length = 20)
    private String financialBaseDate;

    @Column(name = "revenue")
    private Long revenue;

    @Column(name = "revenue_growth_rate")
    private Double revenueGrowthRate;

    @Column(name = "operating_profit")
    private Long operatingProfit;

    @Column(name = "operating_profit_growth_rate")
    private Double operatingProfitGrowthRate;

    @Column(name = "net_profit")
    private Long netProfit;

    @Column(name = "net_profit_growth_rate")
    private Double netProfitGrowthRate;

    @Column(name = "investor_base_date", length = 20)
    private String investorBaseDate;

    @Column(name = "investor_foreign_net")
    private Long investorForeign;

    @Column(name = "investor_institution_net")
    private Long investorInstitution;

    @Column(name = "investor_individual_net")
    private Long investorIndividual;

    @Column(name = "last_refreshed_at", nullable = false)
    private LocalDateTime lastRefreshedAt;

    public static StockCompanySnapshot create(Stock stock) {
        StockCompanySnapshot snapshot = new StockCompanySnapshot();
        snapshot.stock = stock;
        snapshot.industryGroup = "";
        snapshot.subIndustry = "";
        snapshot.sectorTagsJson = "[]";
        snapshot.overview = "";
        snapshot.financialBaseDate = "";
        snapshot.investorBaseDate = "";
        snapshot.marketRank = null;
        return snapshot;
    }

    public void apply(
            String industryGroup,
            String subIndustry,
            String sectorTagsJson,
            long marketCap,
            long totalShares,
            double foreignRatio,
            String overview,
            String financialBaseDate,
            long revenue,
            double revenueGrowthRate,
            long operatingProfit,
            double operatingProfitGrowthRate,
            long netProfit,
            double netProfitGrowthRate,
            String investorBaseDate,
            long investorForeign,
            long investorInstitution,
            long investorIndividual
    ) {
        this.industryGroup = industryGroup == null ? "" : industryGroup;
        this.subIndustry = subIndustry == null ? "" : subIndustry;
        this.sectorTagsJson = sectorTagsJson == null ? "[]" : sectorTagsJson;
        this.marketCap = preserveLong(this.marketCap, marketCap);
        this.totalShares = preserveLong(this.totalShares, totalShares);
        this.foreignRatio = preserveDouble(this.foreignRatio, foreignRatio);
        this.overview = overview == null ? "" : overview;
        this.financialBaseDate = financialBaseDate == null ? "" : financialBaseDate;
        this.revenue = preserveLong(this.revenue, revenue);
        this.revenueGrowthRate = preserveDouble(this.revenueGrowthRate, revenueGrowthRate);
        this.operatingProfit = preserveLong(this.operatingProfit, operatingProfit);
        this.operatingProfitGrowthRate = preserveDouble(this.operatingProfitGrowthRate, operatingProfitGrowthRate);
        this.netProfit = preserveLong(this.netProfit, netProfit);
        this.netProfitGrowthRate = preserveDouble(this.netProfitGrowthRate, netProfitGrowthRate);
        this.investorBaseDate = investorBaseDate == null ? "" : investorBaseDate;
        this.investorForeign = preserveLong(this.investorForeign, investorForeign);
        this.investorInstitution = preserveLong(this.investorInstitution, investorInstitution);
        this.investorIndividual = preserveLong(this.investorIndividual, investorIndividual);
        this.lastRefreshedAt = LocalDateTime.now();
    }

    public void updateMarketRank(Integer marketRank) {
        this.marketRank = marketRank;
    }

    private Long preserveLong(Long currentValue, long incomingValue) {
        if (incomingValue == 0L && currentValue != null) {
            return currentValue;
        }
        return incomingValue;
    }

    private Double preserveDouble(Double currentValue, double incomingValue) {
        if (Math.abs(incomingValue) < 0.000001d && currentValue != null) {
            return currentValue;
        }
        return incomingValue;
    }
}
