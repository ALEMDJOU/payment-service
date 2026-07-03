package com.yowyob.payment.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.yowyob.payment.domain.exception.WalletNotFoundException;
import com.yowyob.payment.domain.user.User;
import com.yowyob.payment.domain.user.UserRepositoryPort;
import com.yowyob.payment.domain.wallet.Wallet;
import com.yowyob.payment.domain.wallet.WalletRepositoryPort;
import com.yowyob.payment.domain.wallet.WalletStatus;
import com.yowyob.payment.infrastructure.adapters.outbound.redis.WalletBalanceCache;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Cas d'usage gestion des portefeuilles.
 */
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepositoryPort walletRepository;
    private final UserRepositoryPort userRepository;
    private final WalletBalanceCache balanceCache;

    @Value("${yowyob.transaction.max-wallet-balance}")
    private BigDecimal maxWalletBalance;

    /**
     * @param userId propriétaire
     * @return portefeuille créé
     */
    public Mono<Wallet> createWallet(UUID userId) {
        Wallet wallet = new Wallet(UUID.randomUUID(), userId, BigDecimal.ZERO,
                WalletStatus.ACTIVE, Instant.now(), Instant.now());
        return walletRepository.save(wallet);
    }

    /**
     * @return tous les portefeuilles enrichis (admin) ou filtrés par user
     */
    public Flux<Wallet> findAll(UUID requesterId, boolean isAdmin) {
        if (isAdmin) {
            return walletRepository.findAll();
        }
        return walletRepository.findByUserId(requesterId);
    }

    /**
     * @param walletId identifiant
     * @return portefeuille
     */
    public Mono<Wallet> findById(UUID walletId) {
        return walletRepository.findById(walletId)
                .switchIfEmpty(Mono.error(new WalletNotFoundException("Portefeuille introuvable")));
    }

    /**
     * @param ownerId identifiant propriétaire
     * @return portefeuille
     */
    public Mono<Wallet> findByOwnerId(UUID ownerId) {
        return walletRepository.findFirstByUserId(ownerId)
                .switchIfEmpty(Mono.error(new WalletNotFoundException("Portefeuille introuvable pour cet owner")));
    }

    /**
     * @param walletId identifiant
     * @param userId   propriétaire attendu (mise à jour métadonnées)
     * @return portefeuille mis à jour
     */
    public Mono<Wallet> updateWallet(UUID walletId, UUID userId) {
        return walletRepository.findById(walletId)
                .switchIfEmpty(Mono.error(new WalletNotFoundException("Portefeuille introuvable")))
                .map(wallet -> new Wallet(wallet.id(), userId, wallet.balance(), wallet.status(),
                        wallet.createdAt(), Instant.now()))
                .flatMap(walletRepository::update);
    }

    /**
     * @param walletId identifiant
     * @return void
     */
    public Mono<Void> deleteWallet(UUID walletId) {
        return walletRepository.deleteById(walletId);
    }

    /**
     * @param walletId identifiant
     * @return true si solde positif
     */
    public Mono<Boolean> canOperate(UUID walletId) {
        return getBalance(walletId).map(balance -> balance.compareTo(BigDecimal.ZERO) > 0);
    }

    /**
     * @param walletId identifiant
     * @return solde (cache Redis puis DB)
     */
    public Mono<BigDecimal> getBalance(UUID walletId) {
        return balanceCache.get(walletId)
                .switchIfEmpty(walletRepository.findById(walletId)
                        .map(Wallet::balance)
                        .flatMap(balance -> balanceCache.put(walletId, balance).thenReturn(balance)));
    }

    /**
     * @param walletId identifiant
     * @param status   nouveau statut
     * @return portefeuille mis à jour
     */
    public Mono<Wallet> updateStatus(UUID walletId, WalletStatus status) {
        return walletRepository.findById(walletId)
                .switchIfEmpty(Mono.error(new WalletNotFoundException("Portefeuille introuvable")))
                .map(wallet -> wallet.withStatus(status))
                .flatMap(walletRepository::update);
    }

    /**
     * @param wallet portefeuille
     * @return nom du propriétaire
     */
    public Mono<String> resolveOwnerName(Wallet wallet) {
        return userRepository.findById(wallet.userId()).map(User::name);
    }

    /**
     * @param walletId identifiant
     * @param userId   utilisateur demandeur
     * @param isAdmin  admin bypass
     * @return portefeuille si autorisé
     */
    public Mono<Wallet> authorizeAccess(UUID walletId, UUID userId, boolean isAdmin) {
        return findById(walletId)
                .flatMap(wallet -> {
                    if (isAdmin || wallet.userId().equals(userId)) {
                        return Mono.just(wallet);
                    }
                    return Mono.error(new WalletNotFoundException("Portefeuille introuvable"));
                });
    }

    /**
     * @param amount montant à ajouter
     * @return true si dans les limites
     */
    public boolean isWithinMaxBalance(BigDecimal current, BigDecimal amount) {
        return current.add(amount).compareTo(maxWalletBalance) <= 0;
    }
}
