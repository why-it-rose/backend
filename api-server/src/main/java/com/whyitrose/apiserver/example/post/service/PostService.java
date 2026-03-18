package com.whyitrose.apiserver.example.post.service;

import com.whyitrose.apiserver.example.post.dto.PostCreateRequest;
import com.whyitrose.apiserver.example.post.dto.PostResponse;
import com.whyitrose.domain.example.post.Post;
import com.whyitrose.domain.example.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;

    @Transactional
    public PostResponse create(PostCreateRequest request) {
        Post post = Post.create(request.title(), request.content());
        return PostResponse.from(postRepository.save(post));
    }

    public List<PostResponse> findAll() {
        return postRepository.findAll().stream()
                .map(PostResponse::from)
                .toList();
    }
}