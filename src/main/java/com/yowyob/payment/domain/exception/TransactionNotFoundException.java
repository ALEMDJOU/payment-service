package com.yowyob.payment.domain.exception;

/**
 * Transaction introuvable.
 */
public class TransactionNotFoundException extends UserFriendlyException {

    /**
     * @param message détail
     */
    public TransactionNotFoundException(String message) {
        super(message);
    }
}
