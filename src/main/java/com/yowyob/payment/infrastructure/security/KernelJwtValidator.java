package com.yowyob.payment.infrastructure.security;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.yowyob.payment.infrastructure.config.KernelAuthProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Valide les JWT kernel RS256 via JWKS distant.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KernelJwtValidator {

    private final KernelAuthProperties properties;
    private final WebClient.Builder webClientBuilder;

    private final Map<String, PublicKey> keyCache = new ConcurrentHashMap<>();
    private volatile Instant keysFetchedAt = Instant.EPOCH;
    private static final long JWKS_CACHE_SECONDS = 300;

    /**
     * @param token JWT compact
     * @return principal extrait des claims
     */
    public Mono<KernelPrincipal> validate(String token) {
        return ensureKeysLoaded()
                .then(Mono.fromCallable(() -> parseAndValidate(token)))
                .onErrorMap(JwtException.class, e -> new JwtException("JWT kernel invalide : " + e.getMessage(), e));
    }

    private KernelPrincipal parseAndValidate(String token) {
        String kid = extractKid(token);
        PublicKey publicKey = keyCache.get(kid);
        if (publicKey == null) {
            throw new JwtException("Clé JWKS introuvable pour kid=" + kid);
        }

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .requireIssuer(properties.getIssuer())
                .build()
                .parseClaimsJws(token)
                .getBody();

        UUID userId = UUID.fromString(claims.getSubject());
        String tenantId = claims.get("tid", String.class);
        String oid = claims.get("oid", String.class);
        if (oid == null || oid.isBlank()) {
            throw new JwtException("Claim oid manquant dans le JWT");
        }
        UUID organizationId = UUID.fromString(oid);
        List<String> permissions = extractPermissions(claims.get("permissions"));

        return new KernelPrincipal(userId, tenantId, organizationId, permissions,
                properties.getPermissions().adminSet());
    }

    private String extractKid(String token) {
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            throw new JwtException("Format JWT invalide");
        }
        try {
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            JsonNode header = new com.fasterxml.jackson.databind.ObjectMapper().readTree(headerJson);
            JsonNode kid = header.get("kid");
            if (kid == null || kid.isNull()) {
                throw new JwtException("Header JWT sans kid");
            }
            return kid.asText();
        } catch (JwtException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new JwtException("Impossible de lire le header JWT", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractPermissions(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        }
        return List.of(raw.toString());
    }

    private Mono<Void> ensureKeysLoaded() {
        if (Instant.now().isBefore(keysFetchedAt.plusSeconds(JWKS_CACHE_SECONDS)) && !keyCache.isEmpty()) {
            return Mono.empty();
        }
        return webClientBuilder.build()
                .get()
                .uri(properties.getJwksUri())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnNext(this::loadKeys)
                .then();
    }

    private void loadKeys(JsonNode jwks) {
        JsonNode keys = jwks.get("keys");
        if (keys == null || !keys.isArray()) {
            throw new JwtException("Réponse JWKS invalide");
        }
        Map<String, PublicKey> newKeys = new ConcurrentHashMap<>();
        for (JsonNode key : keys) {
            if (!"RSA".equals(key.path("kty").asText())) {
                continue;
            }
            String kid = key.path("kid").asText(null);
            String n = key.path("n").asText(null);
            String e = key.path("e").asText(null);
            if (kid == null || n == null || e == null) {
                continue;
            }
            try {
                newKeys.put(kid, buildRsaPublicKey(n, e));
            } catch (Exception ex) {
                log.warn("Impossible de charger la clé JWKS kid={}", kid, ex);
            }
        }
        if (newKeys.isEmpty()) {
            throw new JwtException("Aucune clé RSA dans le JWKS");
        }
        keyCache.clear();
        keyCache.putAll(newKeys);
        keysFetchedAt = Instant.now();
    }

    private PublicKey buildRsaPublicKey(String modulusB64, String exponentB64) throws Exception {
        BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(modulusB64));
        BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(exponentB64));
        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    /**
     * Charge des clés JWKS en mémoire (tests).
     *
     * @param jwks document JWKS
     */
    void loadKeysForTest(JsonNode jwks) {
        loadKeys(jwks);
    }
}
