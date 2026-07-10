package com.yowyob.payment.infrastructure.adapters.inbound.rest.dto;

/**
 * Type d'opération transaction unifiée (POST /api/v1/transactions).
 */
public enum TransactionRequestType {
    RECHARGE,
    WALLET_PAYMENT
}
