package com.whyitrose.domain.news;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NewsTagRepository extends JpaRepository<NewsTag, Long> {

    List<NewsTag> findByNewsId(Long newsId);

    List<NewsTag> findByTagId(Long tagId);
}