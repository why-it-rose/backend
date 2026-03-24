package com.whyitrose.apiserver.auth.oauth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public class OAuth2UserPrincipal implements OAuth2User {
    private final String registrationId;
    private final String providerUid;
    private final String email;
    private final String name;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Map<String, Object> attributes;

    public OAuth2UserPrincipal(
            String registrationId,
            String providerUid,
            String email,
            String name,
            Collection<? extends GrantedAuthority> authorities,
            Map<String, Object> attributes
    ) {
        this.registrationId = registrationId;
        this.providerUid = providerUid;
        this.email = email;
        this.name = name;
        this.authorities = authorities;
        this.attributes = attributes;
    }

    public String getRegistrationId() { return registrationId; }
    public String getProviderUid() { return providerUid; }
    public String getEmail() { return email; }

    @Override
    public String getName() { return name; }

    @Override
    public Map<String, Object> getAttributes() { return attributes; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
}
