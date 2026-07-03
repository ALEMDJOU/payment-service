package com.yowyob.payment.infrastructure.security;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.yowyob.payment.domain.user.User;
import com.yowyob.payment.domain.user.UserRole;
import com.yowyob.payment.domain.user.UserStatus;

/**
 * Tests JWT utilitaires.
 */
class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil(
            "B7lNty52nURS1lCg6KjEvPh6e71c/ndOh1H4mCMRMgo=", 3600000);

    @Test
    void shouldGenerateAndValidateToken() {
        User user = new User(UUID.randomUUID(), "Test", "test@example.com", "hash",
                UserStatus.ACTIVE, UserRole.USER, Instant.now(), Instant.now());
        String token = jwtUtil.generateToken(user);
        assertEquals("test@example.com", jwtUtil.extractUsername(token));
        assertTrue(jwtUtil.validateToken(token, "test@example.com"));
        assertEquals(UserRole.USER, jwtUtil.extractRole(token));
    }
}
