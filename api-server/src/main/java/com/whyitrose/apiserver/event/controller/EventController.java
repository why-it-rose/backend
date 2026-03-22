package com.whyitrose.apiserver.event.controller;

import com.whyitrose.apiserver.event.dto.EventDetailResponse;
import com.whyitrose.apiserver.event.dto.EventDetectRequest;
import com.whyitrose.apiserver.event.dto.EventResponse;
import com.whyitrose.apiserver.event.service.EventService;
import com.whyitrose.core.response.BaseResponse;
import com.whyitrose.domain.event.EventType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Event", description = "이벤트 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    @Operation(
            summary = "이벤트 목록 조회",
            description = "특정 종목의 급등/급락 이벤트 목록을 조회합니다. type 파라미터로 SURGE/DROP 필터링 가능합니다."
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
                                  "eventId": 1,
                                  "stockId": 10,
                                  "stockName": "삼성전자",
                                  "ticker": "005930",
                                  "eventType": "SURGE",
                                  "startDate": "2024-03-16",
                                  "endDate": "2024-03-16",
                                  "changePct": 12.35,
                                  "priceBefore": 70000,
                                  "priceAfter": 78650
                                }
                              ]
                            }
                            """))),
            @ApiResponse(responseCode = "400", description = "stockId 누락 또는 type 값 오류",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": false,
                              "responseCode": 2001,
                              "responseMessage": "요청 파라미터 타입이 올바르지 않습니다."
                            }
                            """))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": false,
                              "responseCode": 5000,
                              "responseMessage": "서버 내부 오류가 발생했습니다."
                            }
                            """)))
    })
    @GetMapping
    public ResponseEntity<BaseResponse<List<EventResponse>>> getEvents(
            @RequestParam Long stockId,
            @RequestParam(required = false) EventType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(BaseResponse.success(eventService.getEvents(stockId, type, page, size)));
    }

    @Operation(
            summary = "이벤트 상세 조회",
            description = "이벤트 상세 정보와 연관 뉴스 목록을 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "responseCode": 1000,
                              "responseMessage": "요청에 성공하였습니다.",
                              "result": {
                                "eventId": 1,
                                "stockId": 10,
                                "stockName": "삼성전자",
                                "ticker": "005930",
                                "eventType": "SURGE",
                                "startDate": "2024-03-16",
                                "endDate": "2024-03-16",
                                "changePct": 12.35,
                                "priceBefore": 70000,
                                "priceAfter": 78650,
                                "summary": "삼성전자가 반도체 수요 급증 소식에 힘입어 12% 급등했습니다.",
                                "newsList": [
                                  {
                                    "newsId": 42,
                                    "title": "삼성전자, 반도체 수요 급증으로 주가 급등",
                                    "source": "한국경제",
                                    "url": "https://www.hankyung.com/article/123456",
                                    "thumbnailUrl": "https://img.hankyung.com/photo/123456.jpg",
                                    "publishedAt": "2024-03-16T09:30:00",
                                    "relevanceScore": 0.9231
                                  }
                                ]
                              }
                            }
                            """))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 이벤트",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": false,
                              "responseCode": 4001,
                              "responseMessage": "존재하지 않는 이벤트입니다."
                            }
                            """))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": false,
                              "responseCode": 5000,
                              "responseMessage": "서버 내부 오류가 발생했습니다."
                            }
                            """)))
    })
    @GetMapping("/{eventId}")
    public ResponseEntity<BaseResponse<EventDetailResponse>> getEventDetail(@PathVariable Long eventId) {
        return ResponseEntity.ok(BaseResponse.success(eventService.getEventDetail(eventId)));
    }

    @Operation(
            summary = "이벤트 탐지 실행 (디버깅용)",
            description = "특정 날짜의 급등/급락 이벤트를 탐지하여 저장합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "탐지 실행 성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "responseCode": 1000,
                              "responseMessage": "요청에 성공하였습니다."
                            }
                            """))),
            @ApiResponse(responseCode = "400", description = "targetDate 누락",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": false,
                              "responseCode": 2001,
                              "responseMessage": "요청 파라미터 타입이 올바르지 않습니다."
                            }
                            """))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": false,
                              "responseCode": 5000,
                              "responseMessage": "서버 내부 오류가 발생했습니다."
                            }
                            """)))
    })
    @PostMapping("/detect")
    public ResponseEntity<BaseResponse<Void>> detect(@RequestBody @Valid EventDetectRequest request) {
        eventService.detectEvents(request.targetDate());
        return ResponseEntity.ok(BaseResponse.success(null));
    }
}
