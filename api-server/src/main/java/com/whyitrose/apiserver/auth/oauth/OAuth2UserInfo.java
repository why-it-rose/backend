package com.whyitrose.apiserver.auth.oauth;

//provider별 응답 형식이 달라서, 이를 공통 인터페이스로
public interface OAuth2UserInfo {
    String getProviderId();
    String getEmail();
    String getName();
}
