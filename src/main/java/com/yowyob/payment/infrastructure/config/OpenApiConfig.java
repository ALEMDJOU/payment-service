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
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

/**
 * Configuration OpenAPI / Swagger (auth kernel).
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
                                                .description(description + """

                                                        ## Authentification kernel

                                                        Toutes les routes métier (sauf `/direct` et Stripe) requièrent :
                                                        - `Authorization: Bearer <JWT RS256>` (obtenu via kernel-core)
                                                        - `X-Client-Id`, `X-Api-Key`, `X-Tenant-Id`, `X-Organization-Id`

                                                        Guide consommateur complet : [/docs/guide.md](/docs/guide.md)
                                                        """)
                                                .version(version)
                                                .contact(new Contact().name("Équipe Backend")
                                                                .email("nzuchuon@gmail.com"))
                                                .license(new License().name("Apache 2.0").url("http://springdoc.org")))
                                .servers(List.of(new Server().url(serverUrl).description("Serveur API")))
                                .tags(List.of(
                                                new Tag().name("Documentation")
                                                                .description("Guide consommateur : [/docs](/docs) → guide.md"),
                                                new Tag().name("Wallets")
                                                                .description("Portefeuilles utilisateur (sub + oid)"),
                                                new Tag().name("Transactions")
                                                                .description("Recharge, paiement wallet, consultation"),
                                                new Tag().name("Stripe")
                                                                .description("Webhooks et callbacks Stripe Checkout")))
                                .addSecurityItem(new SecurityRequirement()
                                                .addList("kernelBearer")
                                                .addList("kernelClientId")
                                                .addList("kernelApiKey")
                                                .addList("kernelTenantId")
                                                .addList("kernelOrganizationId"))
                                .components(new Components()
                                                .addSecuritySchemes("kernelBearer",
                                                                new SecurityScheme()
                                                                                .type(SecurityScheme.Type.HTTP)
                                                                                .scheme("bearer")
                                                                                .bearerFormat("JWT")
                                                                                .description("JWT RS256 émis par kernel-core"))
                                                .addSecuritySchemes("kernelClientId",
                                                                new SecurityScheme()
                                                                                .type(SecurityScheme.Type.APIKEY)
                                                                                .in(SecurityScheme.In.HEADER)
                                                                                .name("X-Client-Id")
                                                                                .description("Identifiant client application kernel"))
                                                .addSecuritySchemes("kernelApiKey",
                                                                new SecurityScheme()
                                                                                .type(SecurityScheme.Type.APIKEY)
                                                                                .in(SecurityScheme.In.HEADER)
                                                                                .name("X-Api-Key")
                                                                                .description("Clé API application kernel"))
                                                .addSecuritySchemes("kernelTenantId",
                                                                new SecurityScheme()
                                                                                .type(SecurityScheme.Type.APIKEY)
                                                                                .in(SecurityScheme.In.HEADER)
                                                                                .name("X-Tenant-Id")
                                                                                .description("Identifiant tenant kernel"))
                                                .addSecuritySchemes("kernelOrganizationId",
                                                                new SecurityScheme()
                                                                                .type(SecurityScheme.Type.APIKEY)
                                                                                .in(SecurityScheme.In.HEADER)
                                                                                .name("X-Organization-Id")
                                                                                .description("Organisation courante (doit correspondre au claim oid du JWT)")));
        }
}
