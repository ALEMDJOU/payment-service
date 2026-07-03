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

import com.yowyob.payment.application.TransactionService;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.DirectTransactionRequest;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.TransactionResponse;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.WalletTransactionRequest;
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
 * API transactions (recharge, wallet-payment, direct).
 */
@Tag(name = "Transaction Management", description = "API for transaction management")
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@OpenApiStandardResponses
public class TransactionController {

        private final TransactionService transactionService;

        /**
         * @param request recharge
         * @return transaction créée
         */
        @PostMapping("/recharge")
        @ResponseStatus(HttpStatus.CREATED)
        @SecurityRequirement(name = "bearerAuth")
        @Operation(summary = "Recharge wallet", description = "Crédite le portefeuille (method=WALLET) ou démarre un Stripe Checkout (method=STRIPE)")
        @ApiResponse(responseCode = "201", description = "Transaction de recharge créée", content = @Content(schema = @Schema(implementation = TransactionResponse.class)))
        public Mono<TransactionResponse> recharge(@Valid @RequestBody WalletTransactionRequest request) {
                return SecurityUtils.currentUserId()
                                .flatMap(userId -> transactionService.recharge(userId, request.walletId(),
                                                request.amount(),
                                                request.method()))
                                .map(TransactionResponse::from);
        }

        /**
         * @param request paiement wallet
         * @return transaction créée
         */
        @PostMapping("/wallet-payment")
        @ResponseStatus(HttpStatus.CREATED)
        @SecurityRequirement(name = "bearerAuth")
        @Operation(summary = "Payment via wallet", description = "Débite le portefeuille avec frais configurables")
        @ApiResponse(responseCode = "201", description = "Transaction de paiement créée", content = @Content(schema = @Schema(implementation = TransactionResponse.class)))
        public Mono<TransactionResponse> walletPayment(@Valid @RequestBody WalletTransactionRequest request) {
                return SecurityUtils.currentUserId()
                                .flatMap(userId -> transactionService.walletPayment(userId, request.walletId(),
                                                request.amount(),
                                                request.method()))
                                .map(TransactionResponse::from);
        }

        /**
         * @param request paiement direct Stripe
         * @return transaction en attente avec session Stripe
         */
        @PostMapping("/direct")
        @ResponseStatus(HttpStatus.CREATED)
        @SecurityRequirement(name = "apiKeyAuth")
        @Operation(summary = "Direct payment", description = "Paiement Stripe sans wallet - requiert le header X-Api-Key (method=STRIPE)")
        @ApiResponse(responseCode = "201", description = "Transaction directe créée avec URL Stripe Checkout", content = @Content(schema = @Schema(implementation = TransactionResponse.class)))
        public Mono<TransactionResponse> direct(@Valid @RequestBody DirectTransactionRequest request) {
                return transactionService.directPayment(request.amount(), request.method(), request.userId())
                                .map(TransactionResponse::from);
        }

        /**
         * @return transactions de l'utilisateur connecté
         */
        @GetMapping
        @SecurityRequirement(name = "bearerAuth")
        @Operation(summary = "List my transactions")
        @ApiResponse(responseCode = "200", description = "Liste des transactions", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TransactionResponse.class))))
        public Flux<TransactionResponse> listMine() {
                return SecurityUtils.currentUserId()
                                .flatMapMany(transactionService::findByUserId)
                                .map(TransactionResponse::from);
        }

        /**
         * @param id identifiant
         * @return transaction
         */
        @GetMapping("/{id}")
        @SecurityRequirement(name = "bearerAuth")
        @Operation(summary = "Get transaction by ID")
        @ApiResponse(responseCode = "200", description = "Transaction trouvée", content = @Content(schema = @Schema(implementation = TransactionResponse.class)))
        public Mono<TransactionResponse> getById(@PathVariable UUID id) {
                return transactionService.findById(id).map(TransactionResponse::from);
        }

        /**
         * @param reference référence métier
         * @return transaction
         */
        @GetMapping("/reference/{reference}")
        @SecurityRequirement(name = "bearerAuth")
        @Operation(summary = "Get transaction by reference")
        @ApiResponse(responseCode = "200", description = "Transaction trouvée", content = @Content(schema = @Schema(implementation = TransactionResponse.class)))
        public Mono<TransactionResponse> getByReference(@PathVariable String reference) {
                return transactionService.findByReference(reference).map(TransactionResponse::from);
        }

        /**
         * @param walletId portefeuille
         * @return historique
         */
        @GetMapping("/wallet/{walletId}")
        @SecurityRequirement(name = "bearerAuth")
        @Operation(summary = "Get transactions by wallet ID")
        @ApiResponse(responseCode = "200", description = "Historique des transactions du portefeuille", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TransactionResponse.class))))
        public Flux<TransactionResponse> getByWallet(@PathVariable UUID walletId) {
                return transactionService.findByWalletId(walletId).map(TransactionResponse::from);
        }
}
