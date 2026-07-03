package com.yowyob.payment.infrastructure.security;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.yowyob.payment.domain.user.User;
import com.yowyob.payment.domain.user.UserRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Génération et validation des jetons JWT stateless.
 */
@Component
public class JwtUtil {

    private final String secret;
    private final long expirationMs;

    /**
     * @param secret       clé secrète Base64
     * @param expirationMs durée de validité en millisecondes
     */
    public JwtUtil(
            @Value("${yowyob.jwt.secret}") String secret,
            @Value("${yowyob.jwt.expiration-ms}") long expirationMs) {
        this.secret = secret;
        this.expirationMs = expirationMs;
    }

    /**
     * @param user utilisateur authentifié
     * @return JWT compact
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.id().toString());
        claims.put("role", user.role().name());
        return buildToken(claims, user.email());
    }

    /**
     * @param token JWT
     * @return email (subject)
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * @param token JWT
     * @return identifiant utilisateur
     */
    public UUID extractUserId(String token) {
        return UUID.fromString(extractAllClaims(token).get("userId", String.class));
    }

    /**
     * @param token JWT
     * @return rôle
     */
    public UserRole extractRole(String token) {
        return UserRole.valueOf(extractAllClaims(token).get("role", String.class));
    }

    /**
     * @param token JWT
     * @param email email attendu
     * @return true si valide et non expiré
     */
    public boolean validateToken(String token, String email) {
        try {
            String subject = extractUsername(token);
            return subject.equals(email) && !isTokenExpired(token);
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * @return durée d'expiration en millisecondes
     */
    public long getExpirationMs() {
        return expirationMs;
    }

    private String buildToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMs))
                .signWith(getSignInKey())
                .compact();
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
