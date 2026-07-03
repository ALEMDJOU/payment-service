package com.yowyob.payment.domain.exception;

/**
 * Portefeuille introuvable.
 */
public class WalletNotFoundException extends UserFriendlyException {

    /**
     * @param message détail
     */
    public WalletNotFoundException(String message) {
        super(message);
    }
}
