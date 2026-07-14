package com.yowyob.payment.infrastructure.adapters.inbound.rest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.yowyob.payment.application.TransactionCheckoutResult;
import com.yowyob.payment.application.TransactionService;
import com.yowyob.payment.domain.exception.TransactionNotFoundException;
import com.yowyob.payment.domain.exception.UnsupportedPaymentMethodException;
import com.yowyob.payment.domain.transaction.PaymentMethod;
import com.yowyob.payment.domain.transaction.Transaction;
import com.yowyob.payment.domain.transaction.TransactionStatus;
import com.yowyob.payment.domain.transaction.TransactionType;
import com.yowyob.payment.infrastructure.security.TestSecurityContext;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Tests unitaires TransactionController via WebTestClient.
 *
 * Scénarios :
 * 1. POST /direct Stripe happy path → 201 + stripeCheckoutUrl
 * 2. POST /direct montant 0 → 400 (validation @Positive)
 * 3. POST /direct method=WALLET → 422 (méthode non supportée)
 * 4. GET /{id} trouvé → 200
 * 5. GET /{id} introuvable → 404
 * 6. GET / liste vide → 200 []
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionController — tests endpoints")
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    private WebTestClient webTestClient;

    private static final UUID USER_ID = TestSecurityContext.TEST_USER_ID;
    private static final UUID ORG_ID  = TestSecurityContext.TEST_ORG_ID;
    private static final UUID TX_ID   = UUID.fromString("cccccccc-0000-0000-0000-000000000003");

    @BeforeEach
    void setUp() {
        TransactionController controller = new TransactionController(transactionService);
        webTestClient = WebTestClient
                .bindToController(controller)
                .controllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Transaction sampleTransaction(TransactionStatus status) {
        return new Transaction(TX_ID, null, USER_ID, ORG_ID,
                new BigDecimal("5000.00"), TransactionType.PAYMENT, status,
                "TXN-001", BigDecimal.ZERO, PaymentMethod.STRIPE,
                "cs_test_abc123", null, Map.of(), Instant.now(), Instant.now());
    }

    private TransactionCheckoutResult pendingCheckout() {
        return new TransactionCheckoutResult(
                sampleTransaction(TransactionStatus.PENDING),
                "https://checkout.stripe.com/pay/cs_test_abc123");
    }

    // ── Scénario 1 — POST /direct Stripe happy path → 201 ────────────────────

    @Test
    @DisplayName("POST /direct — Stripe → 201 avec stripeCheckoutUrl")
    void directPayment_stripe_happyPath_returns201() {
        when(transactionService.directPayment(
                eq(new BigDecimal("5000.00")),
                eq(PaymentMethod.STRIPE),
                any(), any(), any(), any()))
                .thenReturn(Mono.just(pendingCheckout()));

        webTestClient.post()
                .uri("/api/v1/transactions/direct")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Organization-Id", ORG_ID.toString())
                .bodyValue("""
                        {
                          "amount": 5000.00,
                          "method": "STRIPE",
                          "callbackUrl": "https://merchant.example.com/webhooks/payment"
                        }
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(TX_ID.toString())
                .jsonPath("$.status").isEqualTo("PENDING")
                .jsonPath("$.stripeCheckoutUrl")
                        .isEqualTo("https://checkout.stripe.com/pay/cs_test_abc123");
    }

    // ── Scénario 2 — montant 0 → 400 (validation Bean Validation) ────────────

    @Test
    @DisplayName("POST /direct — montant 0 → 400 validation @Positive")
    void directPayment_zeroAmount_returns400() {
        // @Positive sur amount → WebExchangeBindException → 400 avant le service
        webTestClient.post()
                .uri("/api/v1/transactions/direct")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Organization-Id", ORG_ID.toString())
                .bodyValue("""
                        {
                          "amount": 0,
                          "method": "STRIPE"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    // ── Scénario 3 — method=WALLET non supportée → 422 ───────────────────────

    @Test
    @DisplayName("POST /direct — method=WALLET → 422")
    void directPayment_walletMethod_returns422() {
        when(transactionService.directPayment(
                any(), eq(PaymentMethod.WALLET), any(), any(), any(), any()))
                .thenReturn(Mono.error(new UnsupportedPaymentMethodException(PaymentMethod.WALLET)));

        webTestClient.post()
                .uri("/api/v1/transactions/direct")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Organization-Id", ORG_ID.toString())
                .bodyValue("""
                        {
                          "amount": 5000.00,
                          "method": "WALLET"
                        }
                        """)
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.message").exists();
    }

    // ── Scénario 4 — GET /{id} trouvé → 200 ──────────────────────────────────

    @Test
    @DisplayName("GET /{id} — trouvé → 200 avec détails")
    void getById_found_returns200() {
        when(transactionService.authorizeAccess(eq(TX_ID), any(), any(), anyBoolean()))
                .thenReturn(Mono.just(sampleTransaction(TransactionStatus.SUCCEEDED)));

        webTestClient.get()
                .uri("/api/v1/transactions/{id}", TX_ID)
                .header("X-Organization-Id", ORG_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(TX_ID.toString())
                .jsonPath("$.status").isEqualTo("SUCCEEDED")
                .jsonPath("$.amount").isEqualTo(5000.00);
    }

    // ── Scénario 5 — GET /{id} introuvable → 404 ─────────────────────────────

    @Test
    @DisplayName("GET /{id} — introuvable → 404")
    void getById_notFound_returns404() {
        UUID unknownId = UUID.randomUUID();
        when(transactionService.authorizeAccess(eq(unknownId), any(), any(), anyBoolean()))
                .thenReturn(Mono.error(
                        new TransactionNotFoundException("Transaction introuvable: " + unknownId)));

        webTestClient.get()
                .uri("/api/v1/transactions/{id}", unknownId)
                .header("X-Organization-Id", ORG_ID.toString())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").value(msg ->
                        org.assertj.core.api.Assertions.assertThat(msg.toString())
                                .contains("introuvable"));
    }

    // ── Scénario 6 — GET / liste vide → 200 [] ───────────────────────────────

    @Test
    @DisplayName("GET / — liste vide → 200 []")
    void listMine_empty_returns200EmptyArray() {
        when(transactionService.findByUserAndOrganization(any(), any()))
                .thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/api/v1/transactions")
                .header("X-Organization-Id", ORG_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json("[]");
    }
}
