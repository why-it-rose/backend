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

    /** 초당 2건 제한 준수용 요청 간 최소 간격(ms) */
    private long minIntervalMs = 500;

    /** 시드에 없을 때 전량 적재 여부 (시드 파일이 없거나 티커 배열이 비어 있을 때) */
    private boolean fullMasterWhenSeedEmpty = true;
}
