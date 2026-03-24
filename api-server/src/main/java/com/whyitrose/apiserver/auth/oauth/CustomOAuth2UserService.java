package com.whyitrose.apiserver.auth.oauth;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User rawUser = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, rawUser.getAttributes());

        String email = userInfo.getEmail();
        if (email != null) email = email.trim().toLowerCase();

        return new OAuth2UserPrincipal(
                registrationId,
                userInfo.getProviderId(),
                email,
                userInfo.getName(),
                rawUser.getAuthorities(),
                rawUser.getAttributes()
        );
    }
}
