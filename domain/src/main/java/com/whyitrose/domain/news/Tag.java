package com.whyitrose.domain.news;

import com.whyitrose.domain.common.BaseTimeEntity;
import com.whyitrose.domain.common.Status;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "tags",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_tags_name", columnNames = {"name"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // ex) 실적, 정책, 수주
    @Column(name = "name", length = 50, nullable = false)
    private String name;

    // DEFAULT 'ACTIVE'
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status;

    public static Tag create(String name) {
        Tag tag = new Tag();
        tag.name = name;
        tag.status = Status.ACTIVE;
        return tag;
    }

    public void delete() {
        this.status = Status.DELETED;
    }
}