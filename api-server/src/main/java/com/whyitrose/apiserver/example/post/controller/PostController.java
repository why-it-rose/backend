package com.whyitrose.apiserver.example.post.controller;

import com.whyitrose.apiserver.example.post.dto.PostCreateRequest;
import com.whyitrose.apiserver.example.post.dto.PostResponse;
import com.whyitrose.apiserver.example.post.service.PostService;
import com.whyitrose.core.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "[예시] Post", description = "예시용 API입니다. 실제 도메인 작업 시 참고하세요.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/example/posts")
public class PostController {

    private final PostService postService;

    @Operation(summary = "게시글 생성", description = "title과 content를 받아 게시글을 생성합니다.")
    @PostMapping
    public ResponseEntity<BaseResponse<PostResponse>> create(@RequestBody @Valid PostCreateRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success(postService.create(request)));
    }

    @Operation(summary = "게시글 목록 조회", description = "전체 게시글 목록을 반환합니다.")
    @GetMapping
    public ResponseEntity<BaseResponse<List<PostResponse>>> findAll() {
        return ResponseEntity.ok(BaseResponse.success(postService.findAll()));
    }
}