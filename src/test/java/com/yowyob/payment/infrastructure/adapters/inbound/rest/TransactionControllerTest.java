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
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.yowyob.payment.application.TransactionCheckoutResult;
import com.yowyob.payment.application.TransactionService;
import com.yowyob.payment.domain.exception.TransactionNotFoundException;
import com.yowyob.payment.domain.exception.UnsupportedPaymentMethodException;
import com.yowyob.payment.domain.exception.UserFriendlyException;
import com.yowyob.payment.domain.transaction.PaymentMethod;
import com.yowyob.payment.domain.transaction.Transaction;
import com.yowyob.payment.domain.transaction.TransactionStatus;
import com.yowyob.payment.domain.transaction.TransactionType;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Tests unitaires des endpoints TransactionController via WebTestClient.
 *
 * Chaque scénario couvre un cas métier distinct :
 * 1. Paiement direct Stripe — happy path (201 + URL checkout)
 * 2. Paiement direct avec montant nul — validation (400)
 * 3. Paiement direct méthode non supportée — UnsupportedPaymentMethod (422)
 * 4. Consultation transaction par ID — found (200)
 * 5. Consultation transaction par ID — not found (404)
 * 6. Lister les transactions — liste vide (200 [])
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionController — tests endpoints")
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    private WebTestClient webTestClient;

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID ORG_ID  = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    private static final UUID TX_ID   = UUID.fromString("cccccccc-0000-0000-0000-000000000003");

    @BeforeEach
    void setUp() {
        TransactionController controller = new TransactionController(transactionService);
        webTestClient = WebTestClient
                .bindToController(controller)
                .controllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────────────────────────────
    // Scénario 1 — Paiement direct Stripe : happy path → 201 + URL checkout
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /direct — Stripe happy path → 201 avec stripeCheckoutUrl")
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
                .jsonPath("$.stripeCheckoutUrl").isEqualTo("https://checkout.stripe.com/pay/cs_test_abc123")
                .jsonPath("$.method").isEqualTo("STRIPE");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scénario 2 — Montant invalide → 400
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /direct — montant nul → 400 UserFriendlyException")
    void directPayment_zeroAmount_returns400() {
        when(transactionService.directPayment(
                eq(BigDecimal.ZERO), any(), any(), any(), any(), any()))
                .thenReturn(Mono.error(new UserFriendlyException(
                        "Montant invalide : doit être compris entre 100 et 10000000")));

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
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").value(msg ->
                        org.assertj.core.api.Assertions.assertThat(msg.toString())
                                .contains("Montant invalide"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scénario 3 — Méthode non supportée → 422
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /direct — method=WALLET (non supportée pour direct) → 422")
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

    // ─────────────────────────────────────────────────────────────────────────
    // Scénario 4 — GET /{id} : transaction trouvée → 200
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /{id} — transaction existante → 200 avec détails")
    @WithMockUser
    void getById_found_returns200() {
        when(transactionService.authorizeAccess(eq(TX_ID), any(), any(), any()))
                .thenReturn(Mono.just(sampleTransaction(TransactionStatus.SUCCEEDED)));

        webTestClient.get()
                .uri("/api/v1/transactions/{id}", TX_ID)
                .header("Authorization", "Bearer test-token")
                .header("X-Organization-Id", ORG_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(TX_ID.toString())
                .jsonPath("$.status").isEqualTo("SUCCEEDED")
                .jsonPath("$.amount").isEqualTo(5000.00);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scénario 5 — GET /{id} : transaction introuvable → 404
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /{id} — transaction inexistante → 404")
    @WithMockUser
    void getById_notFound_returns404() {
        UUID unknownId = UUID.randomUUID();
        when(transactionService.authorizeAccess(eq(unknownId), any(), any(), any()))
                .thenReturn(Mono.error(
                        new TransactionNotFoundException("Transaction introuvable: " + unknownId)));

        webTestClient.get()
                .uri("/api/v1/transactions/{id}", unknownId)
                .header("Authorization", "Bearer test-token")
                .header("X-Organization-Id", ORG_ID.toString())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").value(msg ->
                        org.assertj.core.api.Assertions.assertThat(msg.toString())
                                .contains("introuvable"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scénario 6 — GET / : liste vide → 200 []
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET / — aucune transaction → 200 liste vide")
    @WithMockUser
    void listMine_empty_returns200EmptyArray() {
        when(transactionService.findByUserAndOrganization(any(), any()))
                .thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/api/v1/transactions")
                .header("Authorization", "Bearer test-token")
                .header("X-Organization-Id", ORG_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json("[]");
    }
}
