package com.yowyob.payment.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.yowyob.payment.domain.event.TransactionEvent;
import com.yowyob.payment.domain.event.TransactionEventPublisherPort;
import com.yowyob.payment.domain.event.WalletEvent;
import com.yowyob.payment.domain.event.WalletEventPublisherPort;
import com.yowyob.payment.domain.exception.InsufficientBalanceException;
import com.yowyob.payment.domain.exception.TransactionNotFoundException;
import com.yowyob.payment.domain.exception.UnsupportedPaymentMethodException;
import com.yowyob.payment.domain.exception.UserFriendlyException;
import com.yowyob.payment.domain.exception.WalletNotFoundException;
import com.yowyob.payment.domain.transaction.PaymentMethod;
import com.yowyob.payment.domain.transaction.Transaction;
import com.yowyob.payment.domain.transaction.TransactionRepositoryPort;
import com.yowyob.payment.domain.transaction.TransactionStateMachine;
import com.yowyob.payment.domain.transaction.TransactionStatus;
import com.yowyob.payment.domain.transaction.TransactionType;
import com.yowyob.payment.domain.wallet.Wallet;
import com.yowyob.payment.domain.wallet.WalletRepositoryPort;
import com.yowyob.payment.domain.webhook.ConsumerWebhookEventType;
import com.yowyob.payment.domain.webhook.ConsumerWebhookNotifierPort;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.TransactionRequest;
import com.yowyob.payment.infrastructure.adapters.outbound.redis.WalletBalanceCache;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Cas d'usage transactions (recharge, paiement wallet, direct Stripe).
 */
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepositoryPort transactionRepository;
    private final WalletRepositoryPort walletRepository;
    private final WalletService walletService;
    private final FeeCalculator feeCalculator;
    private final StripeService stripeService;
    private final TransactionEventPublisherPort transactionEventPublisher;
    private final WalletEventPublisherPort walletEventPublisher;
    private final WalletBalanceCache balanceCache;
    private final TransactionalOperator transactionalOperator;
    private final ConsumerWebhookNotifierPort consumerWebhookNotifier;

    @Value("${yowyob.transaction.reference-prefix}")
    private String referencePrefix;

    @Value("${yowyob.transaction.min-amount}")
    private BigDecimal minAmount;

    @Value("${yowyob.transaction.max-amount}")
    private BigDecimal maxAmount;

    private final AtomicLong referenceSequence = new AtomicLong(System.currentTimeMillis());

    /**
     * Route unifiée POST /transactions (RECHARGE ou WALLET_PAYMENT).
     *
     * @param userId         claim sub
     * @param organizationId claim oid
     * @param request        corps de la requête
     * @return résultat avec URL checkout optionnelle
     */
    public Mono<TransactionCheckoutResult> processTransaction(UUID userId, UUID organizationId,
            TransactionRequest request) {
        return switch (request.type()) {
            case RECHARGE -> recharge(userId, organizationId, request.walletId(), request.amount(),
                    request.method(), request.callbackUrl(), request.metadata());
            case WALLET_PAYMENT -> walletPayment(userId, organizationId, request.walletId(), request.amount(),
                    request.method(), request.callbackUrl(), request.metadata());
        };
    }

    /**
     * @param userId         utilisateur
     * @param organizationId organisation
     * @param walletId       portefeuille
     * @param amount         montant
     * @param method         WALLET (crédit immédiat) ou STRIPE (Checkout)
     * @return transaction de recharge
     */
    public Mono<TransactionCheckoutResult> recharge(UUID userId, UUID organizationId, UUID walletId,
            BigDecimal amount, PaymentMethod method, String callbackUrl, Map<String, String> metadata) {
        validateAmount(amount);
        return ensureSupportedRechargeMethod(method)
                .then(Mono.defer(() -> walletService.authorizeAccess(walletId, userId, organizationId, false)))
                .flatMap(wallet -> createTransaction(walletId, userId, organizationId, amount,
                        TransactionType.RECHARGE, method, BigDecimal.ZERO, callbackUrl, metadata)
                        .flatMap(tx -> {
                            if (method == PaymentMethod.STRIPE) {
                                return startStripeCheckout(tx);
                            }
                            return processWalletCredit(wallet, tx, amount)
                                    .map(result -> new TransactionCheckoutResult(result, null));
                        }));
    }

    /**
     * @param userId         utilisateur
     * @param organizationId organisation
     * @param walletId       portefeuille
     * @param amount         montant
     * @return transaction de paiement via wallet
     */
    public Mono<TransactionCheckoutResult> walletPayment(UUID userId, UUID organizationId, UUID walletId,
            BigDecimal amount, PaymentMethod method, String callbackUrl, Map<String, String> metadata) {
        validateAmount(amount);
        if (method != PaymentMethod.WALLET) {
            return Mono.error(new UnsupportedPaymentMethodException(method));
        }
        BigDecimal fees = feeCalculator.calculate(amount);
        BigDecimal total = amount.add(fees);
        return walletService.authorizeAccess(walletId, userId, organizationId, false)
                .flatMap(wallet -> createTransaction(walletId, userId, organizationId, amount,
                        TransactionType.PAYMENT, PaymentMethod.WALLET, fees, callbackUrl, metadata)
                        .flatMap(tx -> processWalletDebit(wallet, tx, total)
                                .map(result -> new TransactionCheckoutResult(result, null))));
    }

    /**
     * @param amount         montant
     * @param method         méthode (STRIPE en v1)
     * @param userId         utilisateur optionnel
     * @param organizationId organisation (header ou body)
     * @return transaction directe avec URL Checkout
     */
    public Mono<TransactionCheckoutResult> directPayment(BigDecimal amount, PaymentMethod method, UUID userId,
            UUID organizationId, String callbackUrl, Map<String, String> metadata) {
        validateAmount(amount);
        if (method != PaymentMethod.STRIPE) {
            return Mono.error(new UnsupportedPaymentMethodException(method));
        }
        BigDecimal fees = feeCalculator.calculate(amount);
        return createTransaction(null, userId, organizationId, amount, TransactionType.PAYMENT, method, fees,
                callbackUrl, metadata)
                .flatMap(this::startStripeCheckout);
    }

    /**
     * Finalise un paiement Stripe après webhook (recharge wallet ou paiement
     * direct).
     *
     * @param txId identifiant transaction
     * @return transaction finalisée
     */
    public Mono<Transaction> completeStripePayment(UUID txId) {
        return transactionRepository.findById(txId)
                .switchIfEmpty(Mono.error(new TransactionNotFoundException("Transaction introuvable: " + txId)))
                .flatMap(tx -> {
                    if (tx.status() == TransactionStatus.SUCCESSED) {
                        return Mono.just(tx);
                    }
                    if (tx.status() != TransactionStatus.PENDING) {
                        return Mono.error(new UserFriendlyException(
                                "Impossible de finaliser le paiement Stripe : statut actuel "
                                        + tx.status() + ", attendu PENDING"));
                    }
                    if (tx.type() == TransactionType.RECHARGE && tx.walletId() != null) {
                        return walletRepository.findById(tx.walletId())
                                .switchIfEmpty(Mono.error(new WalletNotFoundException(
                                        "Portefeuille introuvable: " + tx.walletId())))
                                .flatMap(wallet -> processWalletCredit(wallet, tx, tx.amount()));
                    }
                    return finalizeSuccess(tx);
                });
    }

    /**
     * @param id             identifiant
     * @param userId         utilisateur
     * @param organizationId organisation
     * @param isAdmin        admin bypass
     * @return transaction si autorisée
     */
    public Mono<Transaction> authorizeAccess(UUID id, UUID userId, UUID organizationId, boolean isAdmin) {
        return findById(id)
                .flatMap(tx -> {
                    if (isAdmin || belongsTo(tx, userId, organizationId)) {
                        return Mono.just(tx);
                    }
                    return Mono.error(new TransactionNotFoundException("Transaction introuvable"));
                });
    }

    /**
     * @param id identifiant
     * @return transaction
     */
    public Mono<Transaction> findById(UUID id) {
        return transactionRepository.findById(id)
                .switchIfEmpty(Mono.error(new TransactionNotFoundException("Transaction introuvable: " + id)));
    }

    /**
     * @param reference      référence
     * @param userId         utilisateur
     * @param organizationId organisation
     * @param isAdmin        admin bypass
     * @return transaction si autorisée
     */
    public Mono<Transaction> findByReferenceAuthorized(String reference, UUID userId, UUID organizationId,
            boolean isAdmin) {
        return transactionRepository.findByReference(reference)
                .switchIfEmpty(Mono.error(
                        new TransactionNotFoundException("Transaction introuvable pour la référence: " + reference)))
                .flatMap(tx -> {
                    if (isAdmin || belongsTo(tx, userId, organizationId)) {
                        return Mono.just(tx);
                    }
                    return Mono.error(new TransactionNotFoundException("Transaction introuvable"));
                });
    }

    /**
     * @param walletId       portefeuille
     * @param userId         utilisateur
     * @param organizationId organisation
     * @param isAdmin        admin bypass
     * @return transactions du portefeuille
     */
    public Flux<Transaction> findByWalletIdAuthorized(UUID walletId, UUID userId, UUID organizationId,
            boolean isAdmin) {
        return walletService.authorizeAccess(walletId, userId, organizationId, isAdmin)
                .thenMany(transactionRepository.findByWalletId(walletId));
    }

    /**
     * @param userId         utilisateur
     * @param organizationId organisation
     * @return transactions du couple (sub, oid)
     */
    public Flux<Transaction> findByUserAndOrganization(UUID userId, UUID organizationId) {
        return transactionRepository.findByUserIdAndOrganizationId(userId, organizationId);
    }

    /**
     * @param transactionId identifiant
     * @param target        statut cible
     * @return transaction mise à jour
     */
    public Mono<Transaction> transitionStatus(UUID transactionId, TransactionStatus target) {
        return transactionRepository.findById(transactionId)
                .switchIfEmpty(
                        Mono.error(new TransactionNotFoundException("Transaction introuvable: " + transactionId)))
                .map(tx -> TransactionStateMachine.transition(tx, target))
                .flatMap(transactionRepository::update)
                .flatMap(updated -> afterTransactionUpdate(updated, null, consumerEventForStatus(target)));
    }

    private boolean belongsTo(Transaction tx, UUID userId, UUID organizationId) {
        return tx.organizationId().equals(organizationId)
                && (tx.userId() == null || tx.userId().equals(userId));
    }

    private Mono<TransactionCheckoutResult> startStripeCheckout(Transaction tx) {
        Transaction pending = TransactionStateMachine.transition(tx, TransactionStatus.PENDING);
        return transactionRepository.update(pending)
                .flatMap(updated -> stripeService.createCheckoutSession(updated)
                        .flatMap(session -> {
                            Transaction withSession = updated.withStripeSessionId(session.getId());
                            return transactionRepository.update(withSession)
                                    .flatMap(saved -> afterTransactionUpdate(saved, session.getUrl(),
                                            ConsumerWebhookEventType.TRANSACTION_PENDING))
                                    .map(saved -> new TransactionCheckoutResult(saved, session.getUrl()));
                        }));
    }

    private Mono<Transaction> createTransaction(UUID walletId, UUID userId, UUID organizationId, BigDecimal amount,
            TransactionType type, PaymentMethod method, BigDecimal fees, String callbackUrl,
            Map<String, String> metadata) {
        Map<String, String> safeMetadata = metadata == null ? Collections.emptyMap() : Map.copyOf(metadata);
        Transaction tx = new Transaction(UUID.randomUUID(), walletId, userId, organizationId, amount, type,
                TransactionStatus.CREATED, nextReference(), fees, method, null, callbackUrl, safeMetadata,
                Instant.now(), Instant.now());
        return transactionRepository.save(tx).flatMap(saved -> publishKafkaEvent(saved).thenReturn(saved));
    }

    private Mono<Transaction> processWalletCredit(Wallet wallet, Transaction tx, BigDecimal amount) {
        if (!walletService.isWithinMaxBalance(wallet.balance(), amount)) {
            return failTransaction(tx, "Solde maximum dépassé pour le portefeuille " + wallet.id());
        }
        Mono<Transaction> flow = transactionalOperator.transactional(
                walletRepository.credit(wallet.id(), amount)
                        .flatMap(updatedWallet -> publishWalletEvent(wallet, updatedWallet, amount))
                        .then(balanceCache.evict(wallet.id()))
                        .then(finalizeSuccess(tx)));
        return flow;
    }

    private Mono<Transaction> processWalletDebit(Wallet wallet, Transaction tx, BigDecimal total) {
        Mono<Transaction> flow = transactionalOperator.transactional(
                walletRepository.debit(wallet.id(), total)
                        .onErrorMap(InsufficientBalanceException.class, e -> e)
                        .flatMap(updatedWallet -> publishWalletEvent(wallet, updatedWallet, total.negate()))
                        .then(balanceCache.evict(wallet.id()))
                        .then(finalizeSuccess(tx))
                        .onErrorResume(InsufficientBalanceException.class, e -> failTransaction(tx, e.getMessage())));
        return flow;
    }

    private Mono<Transaction> finalizeSuccess(Transaction tx) {
        Transaction succeeded;
        if (tx.status() == TransactionStatus.CREATED) {
            Transaction pending = TransactionStateMachine.transition(tx, TransactionStatus.PENDING);
            succeeded = TransactionStateMachine.transition(pending, TransactionStatus.SUCCESSED);
        } else if (tx.status() == TransactionStatus.PENDING) {
            succeeded = TransactionStateMachine.transition(tx, TransactionStatus.SUCCESSED);
        } else {
            return Mono.error(new UserFriendlyException(
                    "Statut incompatible pour finalisation : " + tx.status()));
        }
        return transactionRepository.update(succeeded)
                .flatMap(updated -> afterTransactionUpdate(updated, null,
                        ConsumerWebhookEventType.TRANSACTION_SUCCEEDED));
    }

    private Mono<Transaction> failTransaction(Transaction tx, String reason) {
        Transaction failed = TransactionStateMachine.canTransition(tx.status(), TransactionStatus.FAILED)
                ? TransactionStateMachine.transition(tx, TransactionStatus.FAILED)
                : tx.withStatus(TransactionStatus.FAILED);
        return transactionRepository.update(failed)
                .flatMap(updated -> afterTransactionUpdate(updated, null, ConsumerWebhookEventType.TRANSACTION_FAILED))
                .flatMap(result -> Mono.error(new UserFriendlyException(reason)));
    }

    private Mono<Void> publishWalletEvent(Wallet before, Wallet after, BigDecimal delta) {
        String eventType = delta.signum() >= 0 ? "WALLET_CREDITED" : "WALLET_DEBITED";
        WalletEvent event = new WalletEvent(eventType, after.id(), after.userId(), after.organizationId(),
                before.balance(), after.balance(), delta.abs(), Instant.now());
        return walletEventPublisher.publish(event);
    }

    private Mono<Transaction> publishKafkaEvent(Transaction tx) {
        String eventType = "TRANSACTION_" + tx.status().name();
        TransactionEvent event = new TransactionEvent(eventType, tx.id(), tx.walletId(), tx.amount(),
                tx.fees(), tx.type(), tx.method(), tx.reference(), Instant.now());
        return transactionEventPublisher.publish(event).thenReturn(tx);
    }

    private Mono<Transaction> afterTransactionUpdate(Transaction tx, String stripeCheckoutUrl,
            ConsumerWebhookEventType consumerEvent) {
        return publishKafkaEvent(tx)
                .flatMap(updated -> consumerEvent == null
                        ? Mono.just(updated)
                        : consumerWebhookNotifier.enqueue(updated, consumerEvent, stripeCheckoutUrl)
                                .thenReturn(updated));
    }

    private ConsumerWebhookEventType consumerEventForStatus(TransactionStatus status) {
        return switch (status) {
            case PENDING -> ConsumerWebhookEventType.TRANSACTION_PENDING;
            case SUCCESSED -> ConsumerWebhookEventType.TRANSACTION_SUCCEEDED;
            case FAILED -> ConsumerWebhookEventType.TRANSACTION_FAILED;
            case CANCELLED -> ConsumerWebhookEventType.TRANSACTION_CANCELLED;
            case CREATED -> null;
        };
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(minAmount) < 0 || amount.compareTo(maxAmount) > 0) {
            throw new UserFriendlyException(
                    "Montant invalide : doit être compris entre " + minAmount + " et " + maxAmount);
        }
    }

    private Mono<Void> ensureSupportedRechargeMethod(PaymentMethod method) {
        if (method == PaymentMethod.MOMO || method == PaymentMethod.PAYPAL) {
            return Mono.error(new UnsupportedPaymentMethodException(method));
        }
        if (method != PaymentMethod.WALLET && method != PaymentMethod.STRIPE) {
            return Mono.error(new UnsupportedPaymentMethodException(method));
        }
        return Mono.empty();
    }

    private String nextReference() {
        return referencePrefix + "-" + referenceSequence.incrementAndGet();
    }
}
