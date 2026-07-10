package com.yowyob.payment.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yowyob.payment.domain.transaction.PaymentMethod;
import com.yowyob.payment.domain.transaction.Transaction;
import com.yowyob.payment.domain.transaction.TransactionStatus;
import com.yowyob.payment.domain.transaction.TransactionType;
import com.yowyob.payment.domain.webhook.ConsumerWebhookEventType;
import com.yowyob.payment.domain.webhook.WebhookOutboxEntry;
import com.yowyob.payment.domain.webhook.WebhookOutboxRepositoryPort;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Tests enfilage webhook consommateur.
 */
@ExtendWith(MockitoExtension.class)
class ConsumerWebhookNotifierTest {

    @Mock
    private WebhookOutboxRepositoryPort outboxRepository;

    private ConsumerWebhookNotifier notifier;

    @BeforeEach
    void setUp() {
        notifier = new ConsumerWebhookNotifier(outboxRepository);
    }

    @Test
    void shouldSkipWhenCallbackUrlMissing() {
        Transaction tx = sampleTransaction(null);
        StepVerifier.create(notifier.enqueue(tx, ConsumerWebhookEventType.TRANSACTION_SUCCEEDED, null))
                .verifyComplete();
    }

    @Test
    void shouldEnqueueWhenCallbackUrlPresent() {
        Transaction tx = sampleTransaction("https://merchant.example.com/hook");
        when(outboxRepository.save(any())).thenAnswer(invocation -> {
            WebhookOutboxEntry entry = invocation.getArgument(0);
            return Mono.just(entry);
        });

        StepVerifier
                .create(notifier.enqueue(tx, ConsumerWebhookEventType.TRANSACTION_PENDING,
                        "https://checkout.stripe.com/x"))
                .verifyComplete();

        verify(outboxRepository).save(any());
    }

    @Test
    void shouldRejectInvalidCallbackUrl() {
        Transaction tx = sampleTransaction("not-a-url");
        assertThrows(IllegalArgumentException.class,
                () -> notifier.enqueue(tx, ConsumerWebhookEventType.TRANSACTION_SUCCEEDED, null).block());
    }

    private Transaction sampleTransaction(String callbackUrl) {
        return new Transaction(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.TEN, TransactionType.PAYMENT, TransactionStatus.PENDING, "TXN-1", BigDecimal.ZERO,
                PaymentMethod.STRIPE, "cs_test", callbackUrl, Map.of("orderId", "1"), Instant.now(), Instant.now());
    }
}
