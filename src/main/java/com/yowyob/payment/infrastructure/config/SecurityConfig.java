package com.yowyob.payment.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import com.yowyob.payment.infrastructure.adapters.inbound.rest.JsonErrorWriter;
import com.yowyob.payment.infrastructure.security.SecurityContextRepository;
import com.yowyob.payment.infrastructure.security.YowyobAuthenticationManager;

import lombok.RequiredArgsConstructor;

/**
 * Configuration Spring Security WebFlux (JWT stateless + routes publiques).
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private static final String UNAUTHORIZED_MESSAGE = "Authentification requise : jeton JWT manquant ou invalide";
        private static final String FORBIDDEN_MESSAGE = "Accès refusé : permissions insuffisantes pour cette ressource";

        private final YowyobAuthenticationManager authenticationManager;
        private final SecurityContextRepository securityContextRepository;
        private final JsonErrorWriter jsonErrorWriter;

        /**
         * @param http builder sécurité
         * @return chaîne de filtres
         */
        @Bean
        public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
                return http
                                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                                .authenticationManager(authenticationManager)
                                .securityContextRepository(securityContextRepository)
                                .authorizeExchange(exchanges -> exchanges
                                                .pathMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                                                .pathMatchers(HttpMethod.POST, "/api/v1/transactions/direct")
                                                .permitAll()
                                                .pathMatchers(HttpMethod.POST, "/api/v1/stripe/webhooks").permitAll()
                                                .pathMatchers(HttpMethod.GET, "/api/v1/stripe/success",
                                                                "/api/v1/stripe/cancel")
                                                .permitAll()
                                                .pathMatchers("/actuator/**", "/swagger-ui.html", "/swagger-ui/**",
                                                                "/v3/api-docs", "/v3/api-docs/**", "/webjars/**")
                                                .permitAll()
                                                .anyExchange().authenticated())
                                .exceptionHandling(spec -> spec
                                                .authenticationEntryPoint((exchange, e) -> jsonErrorWriter.write(
                                                                exchange, HttpStatus.UNAUTHORIZED,
                                                                UNAUTHORIZED_MESSAGE))
                                                .accessDeniedHandler((exchange, e) -> jsonErrorWriter.write(
                                                                exchange, HttpStatus.FORBIDDEN, FORBIDDEN_MESSAGE)))
                                .build();
        }
}
