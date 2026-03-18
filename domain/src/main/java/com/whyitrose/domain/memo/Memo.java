package com.whyitrose.domain.memo;

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
        name = "memos",
        indexes = {
                @Index(name = "idx_memos_user_event", columnList = "user_id, event_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Memo extends BaseTimeEntity {

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

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    // DEFAULT 'ACTIVE'
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status;

    public static Memo create(User user, Event event, String content) {
        Memo memo = new Memo();
        memo.user = user;
        memo.event = event;
        memo.content = content;
        memo.status = Status.ACTIVE;
        return memo;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void delete() {
        this.status = Status.DELETED;
    }
}
