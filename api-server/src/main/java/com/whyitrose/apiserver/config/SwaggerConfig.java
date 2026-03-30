package com.whyitrose.apiserver.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(info = @Info(title = "Why It Rose API", version = "v1"))
@Configuration
public class SwaggerConfig {

    private static final String BEARER_TOKEN_SCHEME = "BearerAuth";

    @Bean
    public OpenAPI openAPI() {
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        return new OpenAPI()
                .components(new Components().addSecuritySchemes(BEARER_TOKEN_SCHEME, bearerScheme))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_TOKEN_SCHEME));
    }
}
