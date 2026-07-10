package com.yowyob.payment.infrastructure.config;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration d'authentification via kernel-core (JWT RS256 + headers).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "yowyob.kernel")
public class KernelAuthProperties {

    private String baseUrl;
    private String clientId;
    private String apiKey;
    private String tenantId;
    private String jwksUri;
    private String issuer;
    private Permissions permissions = new Permissions();

    /**
     * Permissions admin configurables.
     */
    @Getter
    @Setter
    public static class Permissions {
        private String admin = "payments:admin";

        /**
         * @return ensemble des permissions admin
         */
        public Set<String> adminSet() {
            return Arrays.stream(admin.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        }
    }
}
