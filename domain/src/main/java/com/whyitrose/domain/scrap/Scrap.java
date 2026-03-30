package com.whyitrose.domain.scrap;

import com.whyitrose.domain.common.BaseTimeEntity;
import com.whyitrose.domain.common.Status;
import com.whyitrose.domain.event.Event;
import com.whyitrose.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "scraps",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_scraps", columnNames = {"user_id", "event_id"})
        },
        indexes = {
                @Index(name = "idx_scraps_user", columnList = "user_id, created_at DESC")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Scrap extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    // DEFAULT 'ACTIVE'
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status;

    public static Scrap create(User user, Event event) {
        Scrap scrap = new Scrap();
        scrap.user = user;
        scrap.event = event;
        scrap.status = Status.ACTIVE;
        return scrap;
    }

    public void delete() {
        this.status = Status.DELETED;
    }

    public void reactivate() {
        this.status = Status.ACTIVE;
    }
}
