package com.yowyob.payment.domain.exception;

import com.yowyob.payment.domain.transaction.PaymentMethod;

/**
 * Méthode de paiement non supportée en v1.
 */
public class UnsupportedPaymentMethodException extends UserFriendlyException {

    /**
     * @param method méthode demandée
     */
    public UnsupportedPaymentMethodException(PaymentMethod method) {
        super("Méthode de paiement non supportée en v1: " + method);
    }
}
