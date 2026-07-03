package com.yowyob.payment.application;

import java.util.Map;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import com.yowyob.payment.infrastructure.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Cas d'usage authentification.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final com.yowyob.payment.infrastructure.security.YowyobAuthenticationManager authenticationManager;

    /**
     * @param email    email
     * @param password mot de passe
     * @return token et expiration
     */
    public Mono<Map<String, Object>> login(String email, String password) {
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password))
                .switchIfEmpty(Mono.error(new BadCredentialsException("Email ou mot de passe incorrect")))
                .flatMap(auth -> userService.findByEmailInternal(email)
                        .map(user -> Map.<String, Object>of(
                                "token", jwtUtil.generateToken(user),
                                "expiresIn", jwtUtil.getExpirationMs())));
    }
}
