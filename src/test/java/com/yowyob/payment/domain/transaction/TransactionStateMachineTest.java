package com.yowyob.payment.domain.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.yowyob.payment.domain.transaction.exception.InvalidTransitionException;

/**
 * Tests unitaires FSM transaction.
 */
class TransactionStateMachineTest {

    private Transaction base() {
        return new Transaction(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.TEN, TransactionType.PAYMENT, TransactionStatus.CREATED,
                "TXN-1", BigDecimal.ZERO, PaymentMethod.WALLET, null, Instant.now(), Instant.now());
    }

    @Test
    void shouldAllowCreatedToPending() {
        Transaction result = TransactionStateMachine.transition(base(), TransactionStatus.PENDING);
        assertEquals(TransactionStatus.PENDING, result.status());
    }

    @Test
    void shouldAllowPendingToSucceeded() {
        Transaction pending = TransactionStateMachine.transition(base(), TransactionStatus.PENDING);
        Transaction result = TransactionStateMachine.transition(pending, TransactionStatus.SUCCESSED);
        assertEquals(TransactionStatus.SUCCESSED, result.status());
    }

    @Test
    void shouldRejectSucceededToPending() {
        Transaction tx = TransactionStateMachine.transition(
                TransactionStateMachine.transition(base(), TransactionStatus.PENDING),
                TransactionStatus.SUCCESSED);
        assertThrows(InvalidTransitionException.class,
                () -> TransactionStateMachine.transition(tx, TransactionStatus.PENDING));
    }

    @Test
    void canTransitionReturnsFalseForIllegal() {
        assertTrue(TransactionStateMachine.canTransition(TransactionStatus.CREATED, TransactionStatus.PENDING));
        assertTrue(!TransactionStateMachine.canTransition(TransactionStatus.SUCCESSED, TransactionStatus.FAILED));
    }
}
