package com.yowyob.payment.infrastructure.adapters.inbound.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;

import com.yowyob.payment.domain.exception.UnsupportedPaymentMethodException;
import com.yowyob.payment.domain.transaction.PaymentMethod;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.WalletTransactionRequest;

import reactor.test.StepVerifier;

/**
 * Tests gestion centralisée des erreurs.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void shouldReturnExplicitMessageForUnsupportedPaymentMethod() {
        StepVerifier.create(handler.handleUnsupportedPaymentMethod(
                new UnsupportedPaymentMethodException(PaymentMethod.MOMO)))
                .assertNext(response -> {
                    assertEquals(422, response.status());
                    assertEquals("UNPROCESSABLE_ENTITY", response.code());
                    assertTrue(response.message().contains("MOMO"));
                    assertTrue(response.fieldErrors().isEmpty());
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnFieldErrorsForValidationFailure() throws NoSuchMethodException {
        WalletTransactionRequest target = new WalletTransactionRequest(null, null, PaymentMethod.WALLET);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "request");
        bindingResult.addError(new FieldError("request", "amount", "must be greater than 0"));
        bindingResult.addError(new FieldError("request", "walletId", "must not be null"));

        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("dummy", WalletTransactionRequest.class), 0);
        WebExchangeBindException exception = new WebExchangeBindException(parameter, bindingResult);

        StepVerifier.create(handler.handleValidation(exception))
                .assertNext(response -> {
                    assertEquals(400, response.status());
                    assertTrue(response.message().contains("amount"));
                    assertTrue(response.message().contains("walletId"));
                    assertEquals(2, response.fieldErrors().size());
                    assertFalse(response.fieldErrors().isEmpty());
                })
                .verifyComplete();
    }

    @SuppressWarnings("unused")
    private void dummy(WalletTransactionRequest request) {
        // Paramètre cible pour MethodParameter
    }
}
