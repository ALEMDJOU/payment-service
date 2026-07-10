package com.yowyob.payment.infrastructure.adapters.inbound.rest;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import com.yowyob.payment.application.TransactionService;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.DirectTransactionRequest;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.TransactionRequest;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.TransactionResponse;
import com.yowyob.payment.infrastructure.security.KernelHeaders;
import com.yowyob.payment.infrastructure.security.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * API transactions (recharge, paiement wallet, direct).
 */
@Tag(name = "Transactions", description = """
                Recharge, paiement wallet et consultation des transactions.
                **Parcours** : 1) Auth kernel → 2) `GET /wallets/me` → 3) `POST /transactions`.
                Headers JWT : Authorization Bearer + X-Client-Id + X-Api-Key + X-Tenant-Id + X-Organization-Id.
                Guide : [/docs/guide.md](/docs/guide.md)
                """)
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@OpenApiStandardResponses
public class TransactionController {

        private final TransactionService transactionService;

        /**
         * @param request recharge ou paiement wallet
         * @return transaction créée
         */
        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        @SecurityRequirement(name = "kernelBearer")
        @SecurityRequirement(name = "kernelClientId")
        @SecurityRequirement(name = "kernelApiKey")
        @SecurityRequirement(name = "kernelTenantId")
        @SecurityRequirement(name = "kernelOrganizationId")
        @Operation(summary = "Créer une transaction", description = """
                        Transaction unifiée : `RECHARGE` (crédit wallet via WALLET ou Stripe Checkout)
                        ou `WALLET_PAYMENT` (débit wallet avec frais).
                        Exemple recharge Stripe :
                        ```json
                        { "type": "RECHARGE", "walletId": "...", "amount": 1000.00, "method": "STRIPE" }
                        ```
                        """)
        @ApiResponse(responseCode = "201", description = "Transaction créée", content = @Content(schema = @Schema(implementation = TransactionResponse.class)))
        public Mono<TransactionResponse> create(@Valid @RequestBody TransactionRequest request) {
                return SecurityUtils.currentUserId()
                                .zipWith(SecurityUtils.currentOrganizationId())
                                .flatMap(t -> transactionService.processTransaction(t.getT1(), t.getT2(), request))
                                .map(TransactionResponse::from);
        }

        /**
         * @param request paiement direct Stripe
         * @return transaction en attente avec session Stripe
         */
        @PostMapping("/direct")
        @ResponseStatus(HttpStatus.CREATED)
        @SecurityRequirement(name = "kernelClientId")
        @SecurityRequirement(name = "kernelApiKey")
        @SecurityRequirement(name = "kernelTenantId")
        @Operation(summary = "Paiement direct (client credentials)", description = """
                        Paiement Stripe sans wallet — auth **client credentials** uniquement
                        (X-Client-Id + X-Api-Key + X-Tenant-Id, sans Bearer).
                        X-Organization-Id optionnel (défaut via body `organizationId`).
                        """)
        @ApiResponse(responseCode = "201", description = "Transaction directe avec URL Stripe Checkout", content = @Content(schema = @Schema(implementation = TransactionResponse.class)))
        public Mono<TransactionResponse> direct(@Valid @RequestBody DirectTransactionRequest request,
                        ServerWebExchange exchange) {
                return resolveOrganizationId(request.organizationId(), exchange)
                                .flatMap(orgId -> transactionService.directPayment(request.amount(), request.method(),
                                                request.userId(), orgId, request.callbackUrl(), request.metadata()))
                                .map(TransactionResponse::from);
        }

        /**
         * @return transactions du couple (sub, oid) courant
         */
        @GetMapping
        @SecurityRequirement(name = "kernelBearer")
        @SecurityRequirement(name = "kernelClientId")
        @SecurityRequirement(name = "kernelApiKey")
        @SecurityRequirement(name = "kernelTenantId")
        @SecurityRequirement(name = "kernelOrganizationId")
        @Operation(summary = "Lister mes transactions")
        @ApiResponse(responseCode = "200", description = "Liste des transactions", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TransactionResponse.class))))
        public Flux<TransactionResponse> listMine() {
                return SecurityUtils.currentUserId()
                                .zipWith(SecurityUtils.currentOrganizationId())
                                .flatMapMany(t -> transactionService.findByUserAndOrganization(t.getT1(), t.getT2()))
                                .map(TransactionResponse::from);
        }

        /**
         * @param id identifiant
         * @return transaction
         */
        @GetMapping("/{id}")
        @SecurityRequirement(name = "kernelBearer")
        @SecurityRequirement(name = "kernelClientId")
        @SecurityRequirement(name = "kernelApiKey")
        @SecurityRequirement(name = "kernelTenantId")
        @SecurityRequirement(name = "kernelOrganizationId")
        @Operation(summary = "Détail transaction")
        @ApiResponse(responseCode = "200", description = "Transaction trouvée", content = @Content(schema = @Schema(implementation = TransactionResponse.class)))
        public Mono<TransactionResponse> getById(@PathVariable UUID id) {
                return SecurityUtils.currentUserId()
                                .zipWith(SecurityUtils.currentOrganizationId())
                                .zipWith(SecurityUtils.isAdmin())
                                .flatMap(t -> transactionService.authorizeAccess(id, t.getT1().getT1(),
                                                t.getT1().getT2(), t.getT2()))
                                .map(TransactionResponse::from);
        }

        /**
         * @param reference référence métier
         * @return transaction
         */
        @GetMapping("/reference/{reference}")
        @SecurityRequirement(name = "kernelBearer")
        @SecurityRequirement(name = "kernelClientId")
        @SecurityRequirement(name = "kernelApiKey")
        @SecurityRequirement(name = "kernelTenantId")
        @SecurityRequirement(name = "kernelOrganizationId")
        @Operation(summary = "Transaction par référence")
        @ApiResponse(responseCode = "200", description = "Transaction trouvée", content = @Content(schema = @Schema(implementation = TransactionResponse.class)))
        public Mono<TransactionResponse> getByReference(@PathVariable String reference) {
                return SecurityUtils.currentUserId()
                                .zipWith(SecurityUtils.currentOrganizationId())
                                .zipWith(SecurityUtils.isAdmin())
                                .flatMap(t -> transactionService.findByReferenceAuthorized(reference, t.getT1().getT1(),
                                                t.getT1().getT2(), t.getT2()))
                                .map(TransactionResponse::from);
        }

        /**
         * @param walletId portefeuille
         * @return historique
         */
        @GetMapping("/wallet/{walletId}")
        @SecurityRequirement(name = "kernelBearer")
        @SecurityRequirement(name = "kernelClientId")
        @SecurityRequirement(name = "kernelApiKey")
        @SecurityRequirement(name = "kernelTenantId")
        @SecurityRequirement(name = "kernelOrganizationId")
        @Operation(summary = "Transactions d'un portefeuille")
        @ApiResponse(responseCode = "200", description = "Historique des transactions", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TransactionResponse.class))))
        public Flux<TransactionResponse> getByWallet(@PathVariable UUID walletId) {
                return SecurityUtils.currentUserId()
                                .zipWith(SecurityUtils.currentOrganizationId())
                                .zipWith(SecurityUtils.isAdmin())
                                .flatMapMany(t -> transactionService.findByWalletIdAuthorized(walletId,
                                                t.getT1().getT1(), t.getT1().getT2(), t.getT2()))
                                .map(TransactionResponse::from);
        }

        private Mono<UUID> resolveOrganizationId(UUID bodyOrgId, ServerWebExchange exchange) {
                if (bodyOrgId != null) {
                        return Mono.just(bodyOrgId);
                }
                String headerOrg = exchange.getRequest().getHeaders().getFirst(KernelHeaders.ORGANIZATION_ID);
                if (headerOrg != null && !headerOrg.isBlank()) {
                        return Mono.just(UUID.fromString(headerOrg.trim()));
                }
                Object attr = exchange.getAttribute(KernelHeaders.ORGANIZATION_ID);
                if (attr instanceof String s && !s.isBlank()) {
                        return Mono.just(UUID.fromString(s.trim()));
                }
                return Mono.error(new IllegalArgumentException(
                                "organizationId requis : header X-Organization-Id ou champ body"));
        }
}
