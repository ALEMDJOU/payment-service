package com.yowyob.payment.infrastructure.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Charge le contexte de sécurité depuis le header Authorization Bearer.
 */
@Component
@RequiredArgsConstructor
public class SecurityContextRepository implements ServerSecurityContextRepository {

    private final JwtUtil jwtUtil;
    private final com.yowyob.payment.domain.user.UserRepositoryPort userRepository;

    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        return Mono.empty();
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.empty();
        }
        String token = authHeader.substring(7);
        try {
            String email = jwtUtil.extractUsername(token);
            return userRepository.findByEmail(email)
                    .filter(user -> jwtUtil.validateToken(token, email))
                    .map(user -> {
                        AuthPrincipal principal = new AuthPrincipal(user);
                        Authentication auth = new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities());
                        return new SecurityContextImpl(auth);
                    });
        } catch (Exception ex) {
            return Mono.empty();
        }
    }
}
