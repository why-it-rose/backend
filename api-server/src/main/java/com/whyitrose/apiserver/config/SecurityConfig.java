package com.whyitrose.apiserver.config;

import com.whyitrose.apiserver.auth.jwt.JwtAuthenticationFilter;
import com.whyitrose.apiserver.auth.oauth.CustomOAuth2UserService;
import com.whyitrose.apiserver.auth.oauth.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// TODO: 실제 인증/인가 로직으로 교체 필요
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/signup",
                                "/auth/login",
                                "/auth/refresh",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                                ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2AuthenticationSuccessHandler)

                ).addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
