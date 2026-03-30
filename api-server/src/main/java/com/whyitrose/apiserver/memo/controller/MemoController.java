package com.whyitrose.apiserver.memo.controller;

import com.whyitrose.apiserver.memo.dto.MemoCreateRequest;
import com.whyitrose.apiserver.memo.dto.MemoResponse;
import com.whyitrose.apiserver.memo.dto.MemoUpdateRequest;
import com.whyitrose.apiserver.memo.service.MemoService;
import com.whyitrose.core.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "memo-controller", description = "메모 API")
@RestController
@RequiredArgsConstructor
@RequestMapping
public class MemoController {

    private final MemoService memoService;

    @Operation(
            summary = "메모 목록 조회",
            description = "특정 이벤트에 대해 내가 작성한 메모 목록을 최신순으로 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "responseCode": 1000,
                              "responseMessage": "요청에 성공하였습니다.",
                              "result": [
                                {
                                  "memoId": 1,
                                  "content": "HBM 납품 기대감으로 외국인 매수세가 강하게 불은 구간.",
                                  "createdAt": "2026-03-16T10:30:00"
                                }
                              ]
                            }
                            """))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 이벤트",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": false,
                              "responseCode": 4011,
                              "responseMessage": "존재하지 않는 이벤트입니다."
                            }
                            """)))
    })
    @GetMapping("/events/{eventId}/memos")
    public ResponseEntity<BaseResponse<List<MemoResponse>>> getMemos(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long eventId
    ) {
        return ResponseEntity.ok(BaseResponse.success(memoService.getMemos(userId, eventId)));
    }

    @Operation(
            summary = "메모 작성",
            description = "특정 이벤트에 메모를 작성합니다. 최대 300자까지 입력 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "작성 성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "responseCode": 1000,
                              "responseMessage": "요청에 성공하였습니다.",
                              "result": {
                                "memoId": 1,
                                "content": "HBM 납품 기대감으로 외국인 매수세가 강하게 불은 구간.",
                                "createdAt": "2026-03-16T10:30:00"
                              }
                            }
                            """))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 요청 (내용 없음 또는 300자 초과)",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": false,
                              "responseCode": 2001,
                              "responseMessage": "요청 파라미터 타입이 올바르지 않습니다."
                            }
                            """))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 이벤트",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": false,
                              "responseCode": 4011,
                              "responseMessage": "존재하지 않는 이벤트입니다."
                            }
                            """)))
    })
    @PostMapping("/events/{eventId}/memos")
    public ResponseEntity<BaseResponse<MemoResponse>> createMemo(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long eventId,
            @RequestBody @Valid MemoCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(memoService.createMemo(userId, eventId, request)));
    }

    @Operation(
            summary = "메모 수정",
            description = "내가 작성한 메모를 수정합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "responseCode": 1000,
                              "responseMessage": "요청에 성공하였습니다.",
                              "result": {
                                "memoId": 1,
                                "content": "수정된 메모 내용입니다.",
                                "createdAt": "2026-03-16T10:30:00"
                              }
                            }
                            """))),
            @ApiResponse(responseCode = "404", description = "존재하지 않거나 본인 메모 아님",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": false,
                              "responseCode": 4010,
                              "responseMessage": "존재하지 않는 메모입니다."
                            }
                            """)))
    })
    @PutMapping("/memos/{memoId}")
    public ResponseEntity<BaseResponse<MemoResponse>> updateMemo(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long memoId,
            @RequestBody @Valid MemoUpdateRequest request
    ) {
        return ResponseEntity.ok(BaseResponse.success(memoService.updateMemo(userId, memoId, request)));
    }

    @Operation(
            summary = "메모 삭제",
            description = "내가 작성한 메모를 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "responseCode": 1000,
                              "responseMessage": "요청에 성공하였습니다."
                            }
                            """))),
            @ApiResponse(responseCode = "404", description = "존재하지 않거나 본인 메모 아님",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": false,
                              "responseCode": 4010,
                              "responseMessage": "존재하지 않는 메모입니다."
                            }
                            """)))
    })
    @DeleteMapping("/memos/{memoId}")
    public ResponseEntity<BaseResponse<Void>> deleteMemo(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long memoId
    ) {
        memoService.deleteMemo(userId, memoId);
        return ResponseEntity.ok(BaseResponse.success(null));
    }
}
