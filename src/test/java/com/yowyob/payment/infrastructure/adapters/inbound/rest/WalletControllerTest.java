package com.yowyob.payment.infrastructure.adapters.inbound.rest;

import java.math.BigDecimal;
import java.time.Instant;
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

import com.yowyob.payment.application.WalletService;
import com.yowyob.payment.domain.exception.WalletNotFoundException;
import com.yowyob.payment.domain.wallet.Wallet;
import com.yowyob.payment.domain.wallet.WalletStatus;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Tests unitaires WalletController.
 *
 * Scénarios :
 * 1. GET /me — wallet existant (200)
 * 2. GET /me — création lazy (200 wallet créé)
 * 3. GET /{id} — wallet introuvable (404)
 * 4. GET /{id}/balance — solde correct (200)
 * 5. GET / — liste vide (200 [])
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletController — tests endpoints")
class WalletControllerTest {

    @Mock
    private WalletService walletService;

    private WebTestClient webTestClient;

    private static final UUID USER_ID   = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID ORG_ID    = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    private static final UUID WALLET_ID = UUID.fromString("dddddddd-0000-0000-0000-000000000004");

    @BeforeEach
    void setUp() {
        WalletController controller = new WalletController(walletService);
        webTestClient = WebTestClient
                .bindToController(controller)
                .controllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Wallet sampleWallet(BigDecimal balance) {
        return new Wallet(WALLET_ID, USER_ID, ORG_ID, balance, WalletStatus.ACTIVE,
                Instant.now(), Instant.now());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scénario 1 — GET /me : wallet existant → 200
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /me — wallet existant → 200 avec solde")
    @WithMockUser
    void getMyWallet_exists_returns200() {
        when(walletService.getOrCreate(any(), any()))
                .thenReturn(Mono.just(sampleWallet(new BigDecimal("25000.00"))));

        webTestClient.get()
                .uri("/api/v1/wallets/me")
                .header("Authorization", "Bearer test-token")
                .header("X-Organization-Id", ORG_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(WALLET_ID.toString())
                .jsonPath("$.balance").isEqualTo(25000.00)
                .jsonPath("$.status").isEqualTo("ACTIVE");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scénario 2 — GET /me : création lazy → 200 solde à zéro
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /me — wallet absent → création lazy → 200 avec solde 0")
    @WithMockUser
    void getMyWallet_lazyCreate_returns200WithZeroBalance() {
        when(walletService.getOrCreate(any(), any()))
                .thenReturn(Mono.just(sampleWallet(BigDecimal.ZERO)));

        webTestClient.get()
                .uri("/api/v1/wallets/me")
                .header("Authorization", "Bearer test-token")
                .header("X-Organization-Id", ORG_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isEqualTo(0)
                .jsonPath("$.status").isEqualTo("ACTIVE");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scénario 3 — GET /{id} : wallet introuvable → 404
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /{id} — wallet inexistant → 404")
    @WithMockUser
    void getById_notFound_returns404() {
        UUID unknownId = UUID.randomUUID();
        when(walletService.authorizeAccess(eq(unknownId), any(), any(), any()))
                .thenReturn(Mono.error(
                        new WalletNotFoundException("Portefeuille introuvable: " + unknownId)));

        webTestClient.get()
                .uri("/api/v1/wallets/{id}", unknownId)
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
    // Scénario 4 — GET /{id}/balance : solde correct → 200
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /{id}/balance — solde existant → 200")
    @WithMockUser
    void getBalance_found_returns200() {
        when(walletService.authorizeAccess(eq(WALLET_ID), any(), any(), any()))
                .thenReturn(Mono.just(sampleWallet(new BigDecimal("12500.50"))));

        webTestClient.get()
                .uri("/api/v1/wallets/{id}/balance", WALLET_ID)
                .header("Authorization", "Bearer test-token")
                .header("X-Organization-Id", ORG_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isEqualTo(12500.50)
                .jsonPath("$.walletId").isEqualTo(WALLET_ID.toString());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scénario 5 — GET / : liste vide → 200 []
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET / — aucun wallet → 200 liste vide")
    @WithMockUser
    void getAll_empty_returns200EmptyArray() {
        when(walletService.findAll(any(), any(), any()))
                .thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/api/v1/wallets")
                .header("Authorization", "Bearer test-token")
                .header("X-Organization-Id", ORG_ID.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json("[]");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scénario 6 — POST / : créer un wallet explicitement → 201
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST / — création explicite wallet → 201")
    @WithMockUser
    void createWallet_returns201() {
        when(walletService.getOrCreate(any(), any()))
                .thenReturn(Mono.just(sampleWallet(BigDecimal.ZERO)));

        webTestClient.post()
                .uri("/api/v1/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer test-token")
                .header("X-Organization-Id", ORG_ID.toString())
                .bodyValue("{}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(WALLET_ID.toString())
                .jsonPath("$.balance").isEqualTo(0);
    }
}
