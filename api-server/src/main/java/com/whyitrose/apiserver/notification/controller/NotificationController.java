package com.whyitrose.apiserver.notification.controller;

import com.whyitrose.apiserver.notification.dto.response.NotificationGroupResponse;
import com.whyitrose.apiserver.notification.dto.response.NotificationSummaryResponse;
import com.whyitrose.apiserver.notification.dto.response.UnreadCountResponse;
import com.whyitrose.apiserver.notification.service.NotificationService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Notification", description = "알림 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "알림 목록 조회", description = """
            날짜별로 그룹핑된 알림 목록을 반환합니다.
            - days: 최근 N일 조회 (없으면 전체)
            - stockId: 특정 종목 필터
            - read: true=읽음만, false=안읽음만, 없으면 전체
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "responseCode": 1000,
                              "responseMessage": "요청에 성공하였습니다.",
                              "result": [
                                {
                                  "date": "2026.03.30",
                                  "notificationId": 1,
                                  "isRead": false,
                                  "stocks": [
                                    {
                                      "stockId": 10,
                                      "stockName": "삼성전자",
                                      "ticker": "005930",
                                      "newsCount": 2,
                                      "newsList": [
                                        {
                                          "newsId": 100,
                                          "title": "삼성전자 HBM 납품 계약 체결",
                                          "summary": "삼성전자가 엔비디아와 HBM3E 납품 계약을...",
                                          "publishedAt": "2026.03.30 09:00",
                                          "source": "한국경제",
                                          "url": "https://example.com/news/100",
                                          "tags": ["실적", "수주"]
                                        }
                                      ]
                                    }
                                  ]
                                }
                              ]
                            }
                            """)))
    })
    @GetMapping
    public ResponseEntity<BaseResponse<List<NotificationGroupResponse>>> getNotifications(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Long stockId,
            @RequestParam(required = false) Boolean read
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                notificationService.getNotifications(userId, days, stockId, read)));
    }

    @Operation(summary = "알림 요약 목록 조회", description = """
            알림별 요약 정보를 반환합니다. 알림센터 오버레이 및 마이페이지 히스토리용.
            - days: 최근 N일 조회 (없으면 전체)
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "responseCode": 1000,
                              "responseMessage": "요청에 성공하였습니다.",
                              "result": [
                                {
                                  "notificationId": 1,
                                  "date": "2026.03.30",
                                  "stockNames": "삼성전자, SK하이닉스",
                                  "relativeTime": "오늘",
                                  "message": "**삼성전자, SK하이닉스**, 총 **5**건의 뉴스를 확인해보세요.",
                                  "isRead": false
                                }
                              ]
                            }
                            """)))
    })
    @GetMapping("/summary")
    public ResponseEntity<BaseResponse<List<NotificationSummaryResponse>>> getSummary(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer days
    ) {
        return ResponseEntity.ok(BaseResponse.success(notificationService.getSummary(userId, days)));
    }

    @Operation(summary = "미읽음 알림 수 조회", description = "로그인 유저의 읽지 않은 알림 수를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "responseCode": 1000,
                              "responseMessage": "요청에 성공하였습니다.",
                              "result": {
                                "count": 3
                              }
                            }
                            """)))
    })
    @GetMapping("/unread-count")
    public ResponseEntity<BaseResponse<UnreadCountResponse>> getUnreadCount(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(BaseResponse.success(notificationService.getUnreadCount(userId)));
    }

    @Operation(summary = "전체 알림 읽음 처리", description = "로그인 유저의 읽지 않은 알림을 모두 읽음 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "처리 성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "responseCode": 1000,
                              "responseMessage": "요청에 성공하였습니다."
                            }
                            """)))
    })
    @PatchMapping("/read-all")
    public ResponseEntity<BaseResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal Long userId
    ) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(BaseResponse.success(null));
    }

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 처리합니다. 이미 읽은 알림은 중복 처리하지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "처리 성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "responseCode": 1000,
                              "responseMessage": "요청에 성공하였습니다."
                            }
                            """))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 알림",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": false,
                              "responseCode": 2010,
                              "responseMessage": "존재하지 않는 알림입니다."
                            }
                            """))),
            @ApiResponse(responseCode = "403", description = "다른 유저의 알림",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": false,
                              "responseCode": 2011,
                              "responseMessage": "접근 권한이 없는 알림입니다."
                            }
                            """)))
    })
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<BaseResponse<Void>> markAsRead(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long notificationId
    ) {
        notificationService.markAsRead(userId, notificationId);
        return ResponseEntity.ok(BaseResponse.success(null));
    }
}
