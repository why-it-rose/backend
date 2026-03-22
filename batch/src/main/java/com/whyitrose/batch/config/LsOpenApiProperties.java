package com.whyitrose.batch.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ls.openapi")
public class LsOpenApiProperties {

    /** 예: https://openapi.ls-sec.co.kr:8080 */
    private String baseUrl = "https://openapi.ls-sec.co.kr:8080";

    /** Bearer 토큰 (OAuth 발급값). 환경변수로 주입 권장. */
    private String accessToken = "";
    private String seedResource = "classpath:seed/index-universe.json";

    /** t9945 등 마스터 연속조회 시 요청 간 최소 간격(ms). (LS: 초당 2건 등) */
    private long minIntervalMs = 500;

    /**
     * t8451(차트) HTTP 호출 간 최소 간격(ms). 전역(모든 종목·연속조회 페이지 공통)으로 적용.
     * LS 문서상 초당 1건이면 1000 이상 권장.
     */
    private long chartMinIntervalMs = 1000L;

    /** 시드에 없을 때 전량 적재 여부 (시드 파일이 없거나 티커 배열이 비어 있을 때) */
    private boolean fullMasterWhenSeedEmpty = true;
}
