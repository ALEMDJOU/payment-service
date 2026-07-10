package com.yowyob.payment.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.yowyob.payment.domain.exception.WalletNotFoundException;
import com.yowyob.payment.domain.wallet.Wallet;
import com.yowyob.payment.domain.wallet.WalletRepositoryPort;
import com.yowyob.payment.domain.wallet.WalletStatus;
import com.yowyob.payment.infrastructure.adapters.outbound.redis.WalletBalanceCache;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Cas d'usage gestion des portefeuilles (couple userId + organizationId kernel).
 */
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepositoryPort walletRepository;
    private final WalletBalanceCache balanceCache;

    @Value("${yowyob.transaction.max-wallet-balance}")
    private BigDecimal maxWalletBalance;

    /**
     * Crée ou retourne le portefeuille du couple (userId, organizationId).
     *
     * @param userId         claim sub
     * @param organizationId claim oid
     * @return portefeuille existant ou nouvellement créé
     */
    public Mono<Wallet> getOrCreate(UUID userId, UUID organizationId) {
        return walletRepository.findByUserIdAndOrganizationId(userId, organizationId)
                .switchIfEmpty(Mono.defer(() -> {
                    Wallet wallet = new Wallet(UUID.randomUUID(), userId, organizationId,
                            BigDecimal.ZERO, WalletStatus.ACTIVE, Instant.now(), Instant.now());
                    return walletRepository.save(wallet);
                }));
    }

    /**
     * @param userId         propriétaire
     * @param organizationId organisation (filtre optionnel, null = tous)
     * @param isAdmin        admin voit tout
     * @return portefeuilles
     */
    public Flux<Wallet> findAll(UUID userId, UUID organizationId, boolean isAdmin) {
        if (isAdmin) {
            return walletRepository.findAll();
        }
        if (organizationId != null) {
            return walletRepository.findByUserIdAndOrganizationIdFilter(userId, organizationId);
        }
        return walletRepository.findByUserId(userId);
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
     * @param walletId       identifiant
     * @param userId         utilisateur demandeur
     * @param organizationId organisation demandeur
     * @param isAdmin        admin bypass
     * @return portefeuille si autorisé
     */
    public Mono<Wallet> authorizeAccess(UUID walletId, UUID userId, UUID organizationId, boolean isAdmin) {
        return findById(walletId)
                .flatMap(wallet -> {
                    if (isAdmin || (wallet.userId().equals(userId)
                            && wallet.organizationId().equals(organizationId))) {
                        return Mono.just(wallet);
                    }
                    return Mono.error(new WalletNotFoundException("Portefeuille introuvable"));
                });
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
     * @param amount montant à ajouter
     * @return true si dans les limites
     */
    public boolean isWithinMaxBalance(BigDecimal current, BigDecimal amount) {
        return current.add(amount).compareTo(maxWalletBalance) <= 0;
    }
}
