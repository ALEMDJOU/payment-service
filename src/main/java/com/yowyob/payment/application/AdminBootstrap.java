package com.yowyob.payment.application;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.yowyob.payment.domain.user.User;
import com.yowyob.payment.domain.user.UserRepositoryPort;
import com.yowyob.payment.domain.user.UserRole;
import com.yowyob.payment.domain.user.UserStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Crée ou met à jour le super-admin au démarrage.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${yowyob.admin.email}")
    private String adminEmail;

    @Value("${yowyob.admin.password}")
    private String adminPassword;

    @Value("${yowyob.admin.name}")
    private String adminName;

    /**
     * @param event événement de démarrage
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onReady(ApplicationReadyEvent event) {
        userRepository.findByEmail(adminEmail)
                .flatMap(existing -> {
                    User updated = new User(existing.id(), adminName, adminEmail,
                            passwordEncoder.encode(adminPassword), UserStatus.ACTIVE, UserRole.ADMIN,
                            existing.createdAt(), Instant.now());
                    return userRepository.update(updated);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    User admin = new User(UUID.randomUUID(), adminName, adminEmail,
                            passwordEncoder.encode(adminPassword), UserStatus.ACTIVE, UserRole.ADMIN,
                            Instant.now(), Instant.now());
                    return userRepository.save(admin);
                }))
                .doOnSuccess(u -> log.info("Admin bootstrap OK pour {}", adminEmail))
                .doOnError(e -> log.error("Admin bootstrap échoué", e))
                .subscribe();
    }
}
