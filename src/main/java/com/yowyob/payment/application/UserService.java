package com.yowyob.payment.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.yowyob.payment.domain.exception.EmailAlreadyExistsException;
import com.yowyob.payment.domain.exception.UserFriendlyException;
import com.yowyob.payment.domain.exception.UserNotFoundException;
import com.yowyob.payment.domain.user.User;
import com.yowyob.payment.domain.user.UserRepositoryPort;
import com.yowyob.payment.domain.user.UserRole;
import com.yowyob.payment.domain.user.UserStatus;
import com.yowyob.payment.domain.wallet.Wallet;
import com.yowyob.payment.domain.wallet.WalletRepositoryPort;
import com.yowyob.payment.domain.wallet.WalletStatus;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Cas d'usage gestion des utilisateurs.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepositoryPort userRepository;
    private final WalletRepositoryPort walletRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * @param name     nom
     * @param email    email unique
     * @param password mot de passe brut
     * @return utilisateur créé sans mot de passe
     */
    public Mono<User> register(String name, String email, String password) {
        return userRepository.findByEmail(email)
                .flatMap(existing -> Mono.<User>error(new EmailAlreadyExistsException("Email déjà utilisé")))
                .switchIfEmpty(Mono.defer(() -> {
                    User user = new User(UUID.randomUUID(), name, email,
                            passwordEncoder.encode(password), UserStatus.ACTIVE, UserRole.USER,
                            Instant.now(), Instant.now());
                    return userRepository.save(user)
                            .flatMap(saved -> createDefaultWallet(saved).thenReturn(saved.withoutPassword()));
                }));
    }

    /**
     * @param id identifiant
     * @return utilisateur sans mot de passe
     */
    public Mono<User> findById(UUID id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new UserNotFoundException("Utilisateur introuvable")))
                .map(User::withoutPassword);
    }

    /**
     * @param email email
     * @return utilisateur avec mot de passe (usage interne auth)
     */
    public Mono<User> findByEmailInternal(String email) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new UserNotFoundException("Utilisateur introuvable")));
    }

    /**
     * @param userId identifiant
     * @param name   nouveau nom
     * @return utilisateur mis à jour
     */
    public Mono<User> updateProfile(UUID userId, String name) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException("Utilisateur introuvable")))
                .map(user -> new User(user.id(), name, user.email(), user.password(),
                        user.status(), user.role(), user.createdAt(), Instant.now()))
                .flatMap(userRepository::update)
                .map(User::withoutPassword);
    }

    /**
     * @param userId      identifiant
     * @param oldPassword ancien mot de passe
     * @param newPassword nouveau mot de passe
     * @return void
     */
    public Mono<Void> changePassword(UUID userId, String oldPassword, String newPassword) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException("Utilisateur introuvable")))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(oldPassword, user.password())) {
                        return Mono.error(new UserFriendlyException("Mot de passe actuel incorrect"));
                    }
                    User updated = new User(user.id(), user.name(), user.email(),
                            passwordEncoder.encode(newPassword), user.status(), user.role(),
                            user.createdAt(), Instant.now());
                    return userRepository.update(updated).then();
                });
    }

    /**
     * @param userId identifiant
     * @param status nouveau statut
     * @return utilisateur mis à jour
     */
    public Mono<User> updateStatus(UUID userId, UserStatus status) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException("Utilisateur introuvable")))
                .map(user -> new User(user.id(), user.name(), user.email(), user.password(),
                        status, user.role(), user.createdAt(), Instant.now()))
                .flatMap(userRepository::update)
                .map(User::withoutPassword);
    }

    /**
     * @return tous les utilisateurs (admin)
     */
    public Flux<User> findAll() {
        return userRepository.findAll().map(User::withoutPassword);
    }

    private Mono<Wallet> createDefaultWallet(User user) {
        Wallet wallet = new Wallet(UUID.randomUUID(), user.id(), BigDecimal.ZERO,
                WalletStatus.ACTIVE, Instant.now(), Instant.now());
        return walletRepository.save(wallet);
    }
}