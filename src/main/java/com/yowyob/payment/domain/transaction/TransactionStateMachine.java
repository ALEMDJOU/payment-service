package com.yowyob.payment.domain.transaction;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.yowyob.payment.domain.transaction.exception.InvalidTransitionException;

/**
 * Machine à états des transactions - toute mutation de statut passe par cette
 * classe.
 */
public final class TransactionStateMachine {

    private static final Map<TransactionStatus, Set<TransactionStatus>> TRANSITIONS = Map.of(
            TransactionStatus.CREATED, EnumSet.of(
                    TransactionStatus.PENDING, TransactionStatus.FAILED),
            TransactionStatus.PENDING, EnumSet.of(
                    TransactionStatus.SUCCESSED, TransactionStatus.FAILED, TransactionStatus.CANCELLED),
            TransactionStatus.SUCCESSED, EnumSet.noneOf(TransactionStatus.class),
            TransactionStatus.FAILED, EnumSet.noneOf(TransactionStatus.class),
            TransactionStatus.CANCELLED, EnumSet.noneOf(TransactionStatus.class));

    private TransactionStateMachine() {
    }

    /**
     * Valide et applique une transition de statut.
     *
     * @param transaction transaction courante
     * @param target      statut cible
     * @return transaction avec nouveau statut
     * @throws InvalidTransitionException si la transition est illégale
     */
    public static Transaction transition(Transaction transaction, TransactionStatus target) {
        TransactionStatus current = transaction.status();
        Set<TransactionStatus> allowed = TRANSITIONS.getOrDefault(current, EnumSet.noneOf(TransactionStatus.class));
        if (!allowed.contains(target)) {
            throw new InvalidTransitionException(current, target);
        }
        return transaction.withStatus(target);
    }

    /**
     * @param from statut source
     * @param to   statut cible
     * @return true si la transition est autorisée
     */
    public static boolean canTransition(TransactionStatus from, TransactionStatus to) {
        return TRANSITIONS.getOrDefault(from, EnumSet.noneOf(TransactionStatus.class)).contains(to);
    }
}
