package com.yowyob.payment.application;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.yowyob.payment.domain.transaction.FeeType;

/**
 * Calcule les frais de transaction selon la configuration environnement.
 */
@Component
public class FeeCalculator {

    private final boolean feesEnabled;
    private final FeeType feeType;
    private final BigDecimal feeValue;

    /**
     * @param feesEnabled activer les frais
     * @param feeType     FIXED ou PERCENTAGE
     * @param feeValue    valeur du frais
     */
    public FeeCalculator(
            @Value("${yowyob.fees.enabled}") boolean feesEnabled,
            @Value("${yowyob.fees.type}") FeeType feeType,
            @Value("${yowyob.fees.value}") BigDecimal feeValue) {
        this.feesEnabled = feesEnabled;
        this.feeType = feeType;
        this.feeValue = feeValue;
    }

    /**
     * @param amount montant de base
     * @return frais calculés (0 si désactivé)
     */
    public BigDecimal calculate(BigDecimal amount) {
        if (!feesEnabled) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return switch (feeType) {
            case FIXED -> feeValue.setScale(4, RoundingMode.HALF_UP);
            case PERCENTAGE -> amount.multiply(feeValue).setScale(4, RoundingMode.HALF_UP);
        };
    }
}
