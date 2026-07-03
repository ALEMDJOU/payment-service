package com.yowyob.payment.infrastructure.security;

import com.yowyob.payment.domain.user.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Authentification email/mot de passe pour WebFlux.
 */
@Component
@RequiredArgsConstructor
public class YowyobAuthenticationManager implements ReactiveAuthenticationManager {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String email = authentication.getName();
        String rawPassword = authentication.getCredentials().toString();
        return userRepository.findByEmail(email)
                .filter(user -> passwordEncoder.matches(rawPassword, user.password()))
                .map(user -> new UsernamePasswordAuthenticationToken(
                        new AuthPrincipal(user), null, new AuthPrincipal(user).getAuthorities()))
                .cast(Authentication.class);
    }
}
