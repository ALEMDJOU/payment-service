package com.yowyob.payment.domain.transaction.exception;

import com.yowyob.payment.domain.transaction.TransactionStatus;

/**
 * Levée lorsqu'une transition de statut transaction est illégale.
 */
public class InvalidTransitionException extends RuntimeException {

    /**
     * @param from statut actuel
     * @param to   statut demandé
     */
    public InvalidTransitionException(TransactionStatus from, TransactionStatus to) {
        super("Transition illégale de " + from + " vers " + to);
    }
}
