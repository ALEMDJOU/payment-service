package com.yowyob.payment.application;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.yowyob.payment.domain.application.ApplicationRepositoryPort;
import com.yowyob.payment.domain.application.ClientApplication;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gestion des applications API (clés pour paiement direct).
 */
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepositoryPort applicationRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * @param name nom de l'application
     * @return application créée avec clé API en clair (une seule fois)
     */
    public Mono<ApplicationResult> createApplication(String name) {
        String rawKey = generateApiKey();
        ClientApplication app = new ClientApplication(UUID.randomUUID(), name,
                passwordEncoder.encode(rawKey), true, Instant.now());
        return applicationRepository.save(app)
                .map(saved -> new ApplicationResult(saved.id(), saved.name(), rawKey, saved.active(),
                        saved.createdAt()));
    }

    /**
     * @return liste des applications (sans clés)
     */
    public Flux<ApplicationResult> listApplications() {
        return applicationRepository.findAll()
                .map(app -> new ApplicationResult(app.id(), app.name(), null, app.active(), app.createdAt()));
    }

    private String generateApiKey() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
