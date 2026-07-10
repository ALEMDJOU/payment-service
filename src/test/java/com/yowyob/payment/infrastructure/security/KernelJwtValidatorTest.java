package com.yowyob.payment.infrastructure.security;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yowyob.payment.infrastructure.config.KernelAuthProperties;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import reactor.test.StepVerifier;

/**
 * Tests validation JWT kernel RS256 avec JWKS mocké.
 */
class KernelJwtValidatorTest {

    private static final String KID = "test-kid";
    private static final String ISSUER = "https://kernel-core.test.local";

    private KeyPair keyPair;
    private KernelJwtValidator validator;
    private UUID userId;
    private UUID organizationId;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();

        KernelAuthProperties properties = new KernelAuthProperties();
        properties.setIssuer(ISSUER);
        KernelAuthProperties.Permissions permissions = new KernelAuthProperties.Permissions();
        permissions.setAdmin("payments:admin");
        properties.setPermissions(permissions);

        validator = new KernelJwtValidator(properties, org.springframework.web.reactive.function.client.WebClient.builder());
        validator.loadKeysForTest(buildJwks());

        userId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
    }

    @Test
    void shouldValidateTokenAndExtractClaims() {
        String token = buildToken(List.of("payments:read", "payments:admin"));

        StepVerifier.create(validator.validate(token))
                .assertNext(principal -> {
                    assertEquals(userId, principal.getUserId());
                    assertEquals("tenant-test", principal.getTenantId());
                    assertEquals(organizationId, principal.getOrganizationId());
                    assertTrue(principal.getPermissions().contains("payments:admin"));
                    assertTrue(principal.isAdmin());
                })
                .verifyComplete();
    }

    @Test
    void shouldRejectTokenWithInvalidIssuer() {
        String token = Jwts.builder()
                .setHeaderParam("kid", KID)
                .setSubject(userId.toString())
                .setIssuer("https://wrong-issuer.local")
                .claim("tid", "tenant-test")
                .claim("oid", organizationId.toString())
                .claim("permissions", List.of("payments:read"))
                .setExpiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();

        StepVerifier.create(validator.validate(token))
                .expectError()
                .verify();
    }

    private String buildToken(List<String> permissions) {
        return Jwts.builder()
                .setHeaderParam("kid", KID)
                .setSubject(userId.toString())
                .setIssuer(ISSUER)
                .claim("tid", "tenant-test")
                .claim("oid", organizationId.toString())
                .claim("permissions", permissions)
                .setExpiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();
    }

    private ObjectNode buildJwks() {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jwks = mapper.createObjectNode();
        ArrayNode keys = mapper.createArrayNode();
        ObjectNode key = mapper.createObjectNode();
        key.put("kty", "RSA");
        key.put("kid", KID);
        key.put("n", Base64.getUrlEncoder().withoutPadding()
                .encodeToString(toUnsignedBytes(publicKey.getModulus())));
        key.put("e", Base64.getUrlEncoder().withoutPadding()
                .encodeToString(toUnsignedBytes(publicKey.getPublicExponent())));
        keys.add(key);
        jwks.set("keys", keys);
        return jwks;
    }

    private static byte[] toUnsignedBytes(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            return Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return bytes;
    }
}
