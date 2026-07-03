package com.yowyob.payment.infrastructure.adapters.inbound.rest;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.yowyob.payment.application.WalletService;
import com.yowyob.payment.domain.wallet.WalletStatus;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.MessageResponse;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.UpdateWalletStatusRequest;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.WalletBalanceResponse;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.WalletCanOperateResponse;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.WalletRequest;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.WalletResponse;
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
 * API portefeuilles (hey.json + extensions).
 */
@Tag(name = "Wallet Management", description = "API for wallet management")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@OpenApiStandardResponses
public class WalletController {

        private final WalletService walletService;

        /**
         * @param request création
         * @return wallet créé
         */
        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        @Operation(summary = "Create a new wallet", description = "Creates a new wallet for a user.")
        @ApiResponse(responseCode = "201", description = "Portefeuille créé", content = @Content(schema = @Schema(implementation = WalletResponse.class)))
        public Mono<WalletResponse> create(@Valid @RequestBody WalletRequest request) {
                return SecurityUtils.isAdmin()
                                .flatMap(isAdmin -> {
                                        if (isAdmin) {
                                                return walletService.createWallet(request.ownerId());
                                        }
                                        return SecurityUtils.currentUserId()
                                                        .flatMap(userId -> walletService.createWallet(userId));
                                })
                                .flatMap(wallet -> walletService.resolveOwnerName(wallet)
                                                .map(name -> WalletResponse.from(wallet, name)));
        }

        /**
         * @return liste des wallets
         */
        @GetMapping
        @Operation(summary = "Get all wallets", description = "Retrieves wallets (scoped to user unless admin).")
        @ApiResponse(responseCode = "200", description = "Liste des portefeuilles", content = @Content(array = @ArraySchema(schema = @Schema(implementation = WalletResponse.class))))
        public Flux<WalletResponse> getAll() {
                return SecurityUtils.currentUserId()
                                .zipWith(SecurityUtils.isAdmin())
                                .flatMapMany(tuple -> walletService.findAll(tuple.getT1(), tuple.getT2()))
                                .flatMap(wallet -> walletService.resolveOwnerName(wallet)
                                                .map(name -> WalletResponse.from(wallet, name)));
        }

        /**
         * @param id identifiant
         * @return wallet
         */
        @GetMapping("/{id}")
        @Operation(summary = "Get wallet by ID")
        @ApiResponse(responseCode = "200", description = "Portefeuille trouvé", content = @Content(schema = @Schema(implementation = WalletResponse.class)))
        public Mono<WalletResponse> getById(@PathVariable UUID id) {
                return authorizeAndMap(id);
        }

        /**
         * @param id identifiant
         * @return solde
         */
        @GetMapping("/{id}/balance")
        @Operation(summary = "Get wallet balance")
        @ApiResponse(responseCode = "200", description = "Solde du portefeuille", content = @Content(schema = @Schema(implementation = WalletBalanceResponse.class)))
        public Mono<WalletBalanceResponse> getBalance(@PathVariable UUID id) {
                return SecurityUtils.currentUserId().zipWith(SecurityUtils.isAdmin())
                                .flatMap(t -> walletService.authorizeAccess(id, t.getT1(), t.getT2()))
                                .flatMap(w -> walletService.getBalance(id).map(WalletBalanceResponse::new));
        }

        /**
         * @param id identifiant
         * @return true si opération possible
         */
        @GetMapping("/{id}/can-operate")
        @Operation(summary = "Check if wallet can operate")
        @ApiResponse(responseCode = "200", description = "Capacité opérationnelle", content = @Content(schema = @Schema(implementation = WalletCanOperateResponse.class)))
        public Mono<WalletCanOperateResponse> canOperate(@PathVariable UUID id) {
                return walletService.canOperate(id).map(WalletCanOperateResponse::new);
        }

        /**
         * @param ownerId propriétaire
         * @return wallet
         */
        @GetMapping("/owner/{id}")
        @Operation(summary = "Get wallet by owner ID")
        @ApiResponse(responseCode = "200", description = "Portefeuille du propriétaire", content = @Content(schema = @Schema(implementation = WalletResponse.class)))
        public Mono<WalletResponse> getByOwner(@PathVariable("id") UUID ownerId) {
                return walletService.findByOwnerId(ownerId)
                                .flatMap(wallet -> walletService.resolveOwnerName(wallet)
                                                .map(name -> WalletResponse.from(wallet, name)));
        }

        /**
         * @param id      identifiant
         * @param request mise à jour
         * @return wallet
         */
        @PatchMapping("/{id}")
        @Operation(summary = "Update a wallet")
        @ApiResponse(responseCode = "200", description = "Portefeuille mis à jour", content = @Content(schema = @Schema(implementation = WalletResponse.class)))
        public Mono<WalletResponse> update(@PathVariable UUID id, @Valid @RequestBody WalletRequest request) {
                return walletService.updateWallet(id, request.ownerId())
                                .flatMap(wallet -> walletService.resolveOwnerName(wallet)
                                                .map(name -> WalletResponse.from(wallet, name)));
        }

        /**
         * @param id      identifiant
         * @param request nouveau statut
         * @return wallet
         */
        @PatchMapping("/{id}/status")
        @Operation(summary = "Update wallet status")
        @ApiResponse(responseCode = "200", description = "Statut mis à jour", content = @Content(schema = @Schema(implementation = WalletResponse.class)))
        public Mono<WalletResponse> updateStatus(@PathVariable UUID id,
                        @Valid @RequestBody UpdateWalletStatusRequest request) {
                return walletService.updateStatus(id, WalletStatus.valueOf(request.status()))
                                .flatMap(wallet -> walletService.resolveOwnerName(wallet)
                                                .map(name -> WalletResponse.from(wallet, name)));
        }

        /**
         * @param id identifiant
         * @return confirmation
         */
        @DeleteMapping("/{id}")
        @Operation(summary = "Delete a wallet")
        @ApiResponse(responseCode = "200", description = "Portefeuille supprimé", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
        public Mono<MessageResponse> delete(@PathVariable UUID id) {
                return walletService.deleteWallet(id)
                                .thenReturn(new MessageResponse("Portefeuille supprimé avec succès"));
        }

        private Mono<WalletResponse> authorizeAndMap(UUID id) {
                return SecurityUtils.currentUserId().zipWith(SecurityUtils.isAdmin())
                                .flatMap(t -> walletService.authorizeAccess(id, t.getT1(), t.getT2()))
                                .flatMap(wallet -> walletService.resolveOwnerName(wallet)
                                                .map(name -> WalletResponse.from(wallet, name)));
        }
}
