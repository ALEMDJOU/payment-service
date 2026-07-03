package com.yowyob.payment.infrastructure.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Configuration OpenAPI / Swagger.
 */
@Configuration
public class OpenApiConfig {

        @Value("${yowyob.swagger.title}")
        private String title;

        @Value("${yowyob.swagger.description}")
        private String description;

        @Value("${yowyob.swagger.version}")
        private String version;

        @Value("${yowyob.swagger.server-url}")
        private String serverUrl;

        /**
         * @return document OpenAPI
         */
        @Bean
        public OpenAPI openAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title(title)
                                                .description(description)
                                                .version(version)
                                                .contact(new Contact().name("Équipe Backend")
                                                                .email("nzuchuon@gmail.com"))
                                                .license(new License().name("Apache 2.0").url("http://springdoc.org")))
                                .servers(List.of(new Server().url(serverUrl).description("Serveur local")))
                                .components(new Components()
                                                .addSecuritySchemes("bearerAuth",
                                                                new SecurityScheme()
                                                                                .type(SecurityScheme.Type.HTTP)
                                                                                .scheme("bearer")
                                                                                .bearerFormat("JWT"))
                                                .addSecuritySchemes("apiKeyAuth",
                                                                new SecurityScheme()
                                                                                .type(SecurityScheme.Type.APIKEY)
                                                                                .in(SecurityScheme.In.HEADER)
                                                                                .name("X-Api-Key")));
        }
}
