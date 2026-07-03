package com.yowyob.payment.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.stripe.model.checkout.Session;
import com.yowyob.payment.domain.event.TransactionEventPublisherPort;
import com.yowyob.payment.domain.event.WalletEventPublisherPort;
import com.yowyob.payment.domain.exception.UnsupportedPaymentMethodException;
import com.yowyob.payment.domain.transaction.PaymentMethod;
import com.yowyob.payment.domain.transaction.Transaction;
import com.yowyob.payment.domain.transaction.TransactionRepositoryPort;
import com.yowyob.payment.domain.transaction.TransactionStatus;
import com.yowyob.payment.domain.transaction.TransactionType;
import com.yowyob.payment.domain.wallet.Wallet;
import com.yowyob.payment.domain.wallet.WalletRepositoryPort;
import com.yowyob.payment.domain.wallet.WalletStatus;
import com.yowyob.payment.infrastructure.adapters.outbound.redis.WalletBalanceCache;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Tests flux Stripe unifié dans TransactionService.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionServiceStripeTest {

    @Mock
    private TransactionRepositoryPort transactionRepository;
    @Mock
    private WalletRepositoryPort walletRepository;
    @Mock
    private WalletService walletService;
    @Mock
    private FeeCalculator feeCalculator;
    @Mock
    private StripeService stripeService;
    @Mock
    private TransactionEventPublisherPort transactionEventPublisher;
    @Mock
    private WalletEventPublisherPort walletEventPublisher;
    @Mock
    private WalletBalanceCache balanceCache;
    @Mock
    private TransactionalOperator transactionalOperator;

    @InjectMocks
    private TransactionService transactionService;

    private UUID userId;
    private UUID walletId;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(transactionService, "referencePrefix", "YYPAY");
        ReflectionTestUtils.setField(transactionService, "minAmount", new BigDecimal("100"));
        ReflectionTestUtils.setField(transactionService, "maxAmount", new BigDecimal("1000000"));

        userId = UUID.randomUUID();
        walletId = UUID.randomUUID();
        wallet = new Wallet(walletId, userId, BigDecimal.ZERO, WalletStatus.ACTIVE, Instant.now(), Instant.now());

        when(transactionEventPublisher.publish(any())).thenReturn(Mono.empty());
        when(walletEventPublisher.publish(any())).thenReturn(Mono.empty());
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void rechargeWithMomoShouldRejectExplicitly() {
        StepVerifier.create(transactionService.recharge(userId, walletId, new BigDecimal("1000"), PaymentMethod.MOMO))
                .expectError(UnsupportedPaymentMethodException.class)
                .verify();
    }

    @Test
    void rechargeWithStripeShouldCreatePendingTransactionWithCheckoutUrl() {
        Transaction created = sampleTransaction(TransactionStatus.CREATED, TransactionType.RECHARGE,
                PaymentMethod.STRIPE);
        Transaction pending = created.withStatus(TransactionStatus.PENDING);
        Transaction withSession = pending.withStripeSessionId("cs_test_123");
        Session session = new Session();
        session.setId("cs_test_123");
        session.setUrl("https://checkout.stripe.com/pay/cs_test_123");

        when(walletService.authorizeAccess(walletId, userId, false)).thenReturn(Mono.just(wallet));
        when(transactionRepository.save(any())).thenReturn(Mono.just(created));
        when(transactionRepository.update(any())).thenReturn(Mono.just(pending), Mono.just(withSession));
        when(stripeService.createCheckoutSession(any())).thenReturn(Mono.just(session));

        StepVerifier.create(transactionService.recharge(userId, walletId, new BigDecimal("1000"), PaymentMethod.STRIPE))
                .assertNext(result -> {
                    assertEquals(TransactionStatus.PENDING, result.transaction().status());
                    assertEquals("cs_test_123", result.transaction().stripeSessionId());
                    assertEquals("https://checkout.stripe.com/pay/cs_test_123", result.stripeCheckoutUrl());
                })
                .verifyComplete();
    }

    @Test
    void completeStripePaymentShouldCreditWalletForRecharge() {
        Transaction pending = sampleTransaction(TransactionStatus.PENDING, TransactionType.RECHARGE,
                PaymentMethod.STRIPE);
        Wallet credited = wallet.withBalance(new BigDecimal("1000"));

        when(transactionRepository.findById(pending.id())).thenReturn(Mono.just(pending));
        when(walletRepository.findById(walletId)).thenReturn(Mono.just(wallet));
        when(walletService.isWithinMaxBalance(wallet.balance(), pending.amount())).thenReturn(true);
        when(walletRepository.credit(walletId, pending.amount())).thenReturn(Mono.just(credited));
        when(balanceCache.evict(walletId)).thenReturn(Mono.empty());
        when(transactionRepository.update(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(transactionService.completeStripePayment(pending.id()))
                .assertNext(result -> {
                    assertEquals(TransactionStatus.SUCCESSED, result.status());
                    assertEquals(TransactionType.RECHARGE, result.type());
                })
                .verifyComplete();
    }

    @Test
    void completeStripePaymentShouldBeIdempotentWhenAlreadySucceeded() {
        Transaction succeeded = sampleTransaction(TransactionStatus.SUCCESSED, TransactionType.PAYMENT,
                PaymentMethod.STRIPE);
        when(transactionRepository.findById(succeeded.id())).thenReturn(Mono.just(succeeded));

        StepVerifier.create(transactionService.completeStripePayment(succeeded.id()))
                .assertNext(result -> assertEquals(TransactionStatus.SUCCESSED, result.status()))
                .verifyComplete();
    }

    @Test
    void directPaymentWithPaypalShouldRejectExplicitly() {
        StepVerifier.create(transactionService.directPayment(new BigDecimal("500"), PaymentMethod.PAYPAL, userId))
                .expectError(UnsupportedPaymentMethodException.class)
                .verify();
    }

    private Transaction sampleTransaction(TransactionStatus status, TransactionType type, PaymentMethod method) {
        Transaction tx = new Transaction(UUID.randomUUID(), walletId, userId, new BigDecimal("1000"), type, status,
                "YYPAY-1", BigDecimal.ZERO, method, null, Instant.now(), Instant.now());
        assertNotNull(tx.id());
        return tx;
    }
}
