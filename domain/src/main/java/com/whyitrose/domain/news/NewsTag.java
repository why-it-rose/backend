package com.whyitrose.domain.news;

import com.whyitrose.domain.common.BaseTimeEntity;
import com.whyitrose.domain.common.Status;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "news_tags",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_news_tags", columnNames = {"news_id", "tag_id"})
        },
        indexes = {
                @Index(name = "idx_news_tags_tag", columnList = "tag_id, news_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsTag extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "news_id", nullable = false)
    private News news;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    // DEFAULT 'ACTIVE'
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status;

    public static NewsTag create(News news, Tag tag) {
        NewsTag newsTag = new NewsTag();
        newsTag.news = news;
        newsTag.tag = tag;
        newsTag.status = Status.ACTIVE;
        return newsTag;
    }

    public void delete() {
        this.status = Status.DELETED;
    }
}
