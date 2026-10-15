package com.chuseok22.whoim.server.common.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  private static final String BEARER_AUTH = "bearerAuth";

  @Bean
  public OpenAPI openApi() {
    return new OpenAPI()
        .info(new Info()
            .title("WHO I'M API")
            .description("전임자의 경험을, 후임자의 판단으로. WHO I'M 서버 API"))
        .components(new Components()
            .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
  }
}
