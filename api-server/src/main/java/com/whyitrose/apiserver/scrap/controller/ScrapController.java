package com.whyitrose.apiserver.scrap.controller;

import com.whyitrose.apiserver.scrap.dto.ScrapResponse;
import com.whyitrose.apiserver.scrap.service.ScrapService;
import com.whyitrose.core.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.whyitrose.apiserver.scrap.dto.MyScrapSearchPageResponse;

import java.util.List;

@Tag(name = "scrap-controller", description = "스크랩 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ScrapController {

    private final ScrapService scrapService;

    @Operation(summary = "스크랩 추가", description = "이벤트를 스크랩합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "스크랩 성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "responseCode": 1000,
                              "responseMessage": "요청에 성공하였습니다."
                            }
                            """))),
            @ApiResponse(responseCode = "409", description = "이미 스크랩한 이벤트",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": false,
                              "responseCode": 4022,
                              "responseMessage": "이미 스크랩한 이벤트입니다."
                            }
                            """)))
    })
    @PostMapping("/events/{eventId}/scraps")
    public ResponseEntity<BaseResponse<Void>> addScrap(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long eventId
    ) {
        scrapService.addScrap(userId, eventId);
        return ResponseEntity.ok(BaseResponse.success(null));
    }

    @Operation(summary = "스크랩 취소", description = "이벤트 스크랩을 취소합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "스크랩 취소 성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "responseCode": 1000,
                              "responseMessage": "요청에 성공하였습니다."
                            }
                            """))),
            @ApiResponse(responseCode = "404", description = "스크랩 없음",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": false,
                              "responseCode": 4020,
                              "responseMessage": "존재하지 않는 스크랩입니다."
                            }
                            """)))
    })
    @DeleteMapping("/events/{eventId}/scraps")
    public ResponseEntity<BaseResponse<Void>> removeScrap(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long eventId
    ) {
        scrapService.removeScrap(userId, eventId);
        return ResponseEntity.ok(BaseResponse.success(null));
    }

    @Operation(summary = "내 스크랩 목록 조회", description = "내가 스크랩한 이벤트 목록을 최신순으로 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "responseCode": 1000,
                              "responseMessage": "요청에 성공하였습니다.",
                              "result": [
                                {
                                  "scrapId": 1,
                                  "eventId": 1,
                                  "stockId": 10,
                                  "stockName": "삼성전자",
                                  "ticker": "005930",
                                  "eventType": "SURGE",
                                  "changePct": 19.47,
                                  "startDate": "2025-03-22",
                                  "endDate": "2025-03-22",
                                  "scrapedAt": "2026-03-30T10:00:00"
                                }
                              ]
                            }
                            """)))
    })
    @GetMapping("/scraps/my")
    public ResponseEntity<BaseResponse<List<ScrapResponse>>> getMyScraps(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(BaseResponse.success(scrapService.getMyScraps(userId)));
    }

    @GetMapping("/scraps/my/search")
    public ResponseEntity<BaseResponse<MyScrapSearchPageResponse>> searchMyScraps(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt,desc") String sort
    ) {
        return ResponseEntity.ok(
                BaseResponse.success(scrapService.searchMyScraps(userId, q, page, size, sort))
        );
    }

}
