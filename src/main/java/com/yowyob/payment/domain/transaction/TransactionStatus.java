package com.yowyob.payment.domain.transaction;

/**
 * Statut d'une transaction (FSM).
 */
public enum TransactionStatus {
    CREATED,
    PENDING,
    SUCCESSED,
    FAILED,
    CANCELLED
}
