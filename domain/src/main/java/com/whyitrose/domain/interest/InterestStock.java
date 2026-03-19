package com.whyitrose.domain.interest;

import com.whyitrose.domain.common.BaseTimeEntity;
import com.whyitrose.domain.common.Status;
import com.whyitrose.domain.stock.Stock;
import com.whyitrose.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "interest_stocks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_interest_stocks", columnNames = {"user_id", "stock_id"})
        },
        indexes = {
                @Index(name = "idx_interest_stocks_stock", columnList = "stock_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterestStock extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    // DEFAULT 'ACTIVE' / DELETED = 해제된 관심종목 (이력 보관)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status;

    public static InterestStock create(User user, Stock stock) {
        InterestStock interestStock = new InterestStock();
        interestStock.user = user;
        interestStock.stock = stock;
        interestStock.status = Status.ACTIVE;
        return interestStock;
    }

    public void delete() {
        this.status = Status.DELETED;
    }

    public void reactivate() {
        this.status = Status.ACTIVE;
    }
}