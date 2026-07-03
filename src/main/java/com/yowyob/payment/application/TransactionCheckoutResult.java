package com.yowyob.payment.application;

import com.yowyob.payment.domain.transaction.Transaction;

/**
 * Résultat d'une transaction avec URL Stripe Checkout optionnelle.
 */
public record TransactionCheckoutResult(Transaction transaction, String stripeCheckoutUrl) {
}
