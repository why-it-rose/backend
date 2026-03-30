package com.whyitrose.domain.news;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NewsTagRepository extends JpaRepository<NewsTag, Long> {

    List<NewsTag> findByNewsId(Long newsId);

    List<NewsTag> findByTagId(Long tagId);

    // 알림 목록 조회용 — N+1 방지: newsId IN 절로 한번에 조회
    @Query("SELECT nt FROM NewsTag nt JOIN FETCH nt.tag WHERE nt.news.id IN :newsIds AND nt.status = 'ACTIVE'")
    List<NewsTag> findByNewsIdInWithTag(@Param("newsIds") List<Long> newsIds);
}