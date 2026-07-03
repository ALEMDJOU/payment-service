package com.yowyob.payment.domain.exception;

/**
 * Solde insuffisant pour l'opération.
 */
public class InsufficientBalanceException extends UserFriendlyException {

    /**
     * @param message détail
     */
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
