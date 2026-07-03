package com.yowyob.payment.application;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.yowyob.payment.domain.transaction.FeeType;

/**
 * Tests calcul des frais.
 */
class FeeCalculatorTest {

    @Test
    void shouldReturnZeroWhenFeesDisabled() {
        FeeCalculator calculator = new FeeCalculator(false, FeeType.PERCENTAGE, new BigDecimal("0.10"));
        assertEquals(0, calculator.calculate(new BigDecimal("1000")).compareTo(BigDecimal.ZERO));
    }

    @Test
    void shouldCalculatePercentageFee() {
        FeeCalculator calculator = new FeeCalculator(true, FeeType.PERCENTAGE, new BigDecimal("0.02"));
        assertEquals(0, calculator.calculate(new BigDecimal("1000")).compareTo(new BigDecimal("20.0000")));
    }

    @Test
    void shouldCalculateFixedFee() {
        FeeCalculator calculator = new FeeCalculator(true, FeeType.FIXED, new BigDecimal("50"));
        assertEquals(0, calculator.calculate(new BigDecimal("1000")).compareTo(new BigDecimal("50.0000")));
    }
}
