package com.yowyob.payment.application;

import java.math.BigDecimal;
import java.time.Instant;
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

    @Value("${yowyob.transaction.reference-prefix}")
    private String referencePrefix;

    @Value("${yowyob.transaction.min-amount}")
    private BigDecimal minAmount;

    @Value("${yowyob.transaction.max-amount}")
    private BigDecimal maxAmount;

    private final AtomicLong referenceSequence = new AtomicLong(System.currentTimeMillis());

    /**
     * @param userId   utilisateur
     * @param walletId portefeuille
     * @param amount   montant
     * @param method   WALLET (crédit immédiat) ou STRIPE (Checkout)
     * @return transaction de recharge
     */
    public Mono<TransactionCheckoutResult> recharge(UUID userId, UUID walletId, BigDecimal amount,
            PaymentMethod method) {
        validateAmount(amount);
        return ensureSupportedRechargeMethod(method)
                .then(Mono.defer(() -> walletService.authorizeAccess(walletId, userId, false)))
                .flatMap(wallet -> createTransaction(walletId, userId, amount, TransactionType.RECHARGE, method,
                        BigDecimal.ZERO)
                        .flatMap(tx -> {
                            if (method == PaymentMethod.STRIPE) {
                                return startStripeCheckout(tx);
                            }
                            return processWalletCredit(wallet, tx, amount)
                                    .map(result -> new TransactionCheckoutResult(result, null));
                        }));
    }

    /**
     * @param userId   utilisateur
     * @param walletId portefeuille
     * @param amount   montant
     * @return transaction de paiement via wallet
     */
    public Mono<TransactionCheckoutResult> walletPayment(UUID userId, UUID walletId, BigDecimal amount,
            PaymentMethod method) {
        validateAmount(amount);
        if (method != PaymentMethod.WALLET) {
            return Mono.error(new UnsupportedPaymentMethodException(method));
        }
        BigDecimal fees = feeCalculator.calculate(amount);
        BigDecimal total = amount.add(fees);
        return walletService.authorizeAccess(walletId, userId, false)
                .flatMap(wallet -> createTransaction(walletId, userId, amount, TransactionType.PAYMENT,
                        PaymentMethod.WALLET, fees)
                        .flatMap(tx -> processWalletDebit(wallet, tx, total)
                                .map(result -> new TransactionCheckoutResult(result, null))));
    }

    /**
     * @param amount montant
     * @param method méthode (STRIPE en v1)
     * @param userId utilisateur optionnel
     * @return transaction directe avec URL Checkout
     */
    public Mono<TransactionCheckoutResult> directPayment(BigDecimal amount, PaymentMethod method, UUID userId) {
        validateAmount(amount);
        if (method != PaymentMethod.STRIPE) {
            return Mono.error(new UnsupportedPaymentMethodException(method));
        }
        BigDecimal fees = feeCalculator.calculate(amount);
        return createTransaction(null, userId, amount, TransactionType.PAYMENT, method, fees)
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
     * @param id identifiant
     * @return transaction
     */
    public Mono<Transaction> findById(UUID id) {
        return transactionRepository.findById(id)
                .switchIfEmpty(Mono.error(new TransactionNotFoundException("Transaction introuvable: " + id)));
    }

    /**
     * @param reference référence
     * @return transaction
     */
    public Mono<Transaction> findByReference(String reference) {
        return transactionRepository.findByReference(reference)
                .switchIfEmpty(Mono.error(
                        new TransactionNotFoundException("Transaction introuvable pour la référence: " + reference)));
    }

    /**
     * @param walletId portefeuille
     * @return transactions
     */
    public Flux<Transaction> findByWalletId(UUID walletId) {
        return transactionRepository.findByWalletId(walletId);
    }

    /**
     * @param userId utilisateur
     * @return transactions
     */
    public Flux<Transaction> findByUserId(UUID userId) {
        return transactionRepository.findByUserId(userId);
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
                .flatMap(this::publishTransactionEvent);
    }

    private Mono<TransactionCheckoutResult> startStripeCheckout(Transaction tx) {
        Transaction pending = TransactionStateMachine.transition(tx, TransactionStatus.PENDING);
        return transactionRepository.update(pending)
                .flatMap(updated -> stripeService.createCheckoutSession(updated)
                        .flatMap(session -> {
                            Transaction withSession = updated.withStripeSessionId(session.getId());
                            return transactionRepository.update(withSession)
                                    .flatMap(this::publishTransactionEvent)
                                    .map(saved -> new TransactionCheckoutResult(saved, session.getUrl()));
                        }));
    }

    private Mono<Transaction> createTransaction(UUID walletId, UUID userId, BigDecimal amount,
            TransactionType type, PaymentMethod method, BigDecimal fees) {
        Transaction tx = new Transaction(UUID.randomUUID(), walletId, userId, amount, type,
                TransactionStatus.CREATED, nextReference(), fees, method, null, Instant.now(), Instant.now());
        return transactionRepository.save(tx).flatMap(this::publishTransactionEvent);
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
        return transactionRepository.update(succeeded).flatMap(this::publishTransactionEvent);
    }

    private Mono<Transaction> failTransaction(Transaction tx, String reason) {
        Transaction failed = TransactionStateMachine.canTransition(tx.status(), TransactionStatus.FAILED)
                ? TransactionStateMachine.transition(tx, TransactionStatus.FAILED)
                : tx.withStatus(TransactionStatus.FAILED);
        return transactionRepository.update(failed)
                .flatMap(this::publishTransactionEvent)
                .flatMap(result -> Mono.error(new UserFriendlyException(reason)));
    }

    private Mono<Void> publishWalletEvent(Wallet before, Wallet after, BigDecimal delta) {
        String eventType = delta.signum() >= 0 ? "WALLET_CREDITED" : "WALLET_DEBITED";
        WalletEvent event = new WalletEvent(eventType, after.id(), after.userId(),
                before.balance(), after.balance(), delta.abs(), Instant.now());
        return walletEventPublisher.publish(event);
    }

    private Mono<Transaction> publishTransactionEvent(Transaction tx) {
        String eventType = "TRANSACTION_" + tx.status().name();
        TransactionEvent event = new TransactionEvent(eventType, tx.id(), tx.walletId(), tx.amount(),
                tx.fees(), tx.type(), tx.method(), tx.reference(), Instant.now());
        return transactionEventPublisher.publish(event).thenReturn(tx);
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
