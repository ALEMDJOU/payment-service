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

import com.yowyob.payment.application.WalletService;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.WalletBalanceResponse;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.WalletCreateRequest;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.WalletResponse;
import com.yowyob.payment.infrastructure.security.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * API portefeuilles (kernel sub + oid).
 */
@Tag(name = "Wallets", description = """
        Cycle de vie des portefeuilles utilisateur.
        **Parcours** : 1) Auth kernel → 2) `GET /wallets/me` (création lazy) → 3) transactions.
        Headers requis : Authorization Bearer + X-Client-Id + X-Api-Key + X-Tenant-Id + X-Organization-Id.
        Guide complet : [/docs/guide.md](/docs/guide.md)
        """)
@SecurityRequirement(name = "kernelBearer")
@SecurityRequirement(name = "kernelClientId")
@SecurityRequirement(name = "kernelApiKey")
@SecurityRequirement(name = "kernelTenantId")
@SecurityRequirement(name = "kernelOrganizationId")
@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@OpenApiStandardResponses
public class WalletController {

        private final WalletService walletService;

        /**
         * @return wallet du couple (sub, oid) courant ; création lazy si absent
         */
        @GetMapping("/me")
        @Operation(summary = "Mon portefeuille courant", description = """
                Retourne le portefeuille du couple (sub, oid) issu du JWT et du header X-Organization-Id.
                **Crée automatiquement** le portefeuille s'il n'existe pas encore (lazy create).
                """)
        @ApiResponse(responseCode = "200", description = "Portefeuille trouvé ou créé",
                content = @Content(schema = @Schema(implementation = WalletResponse.class)))
        public Mono<WalletResponse> getMine() {
                return SecurityUtils.currentUserId()
                                .zipWith(SecurityUtils.currentOrganizationId())
                                .flatMap(t -> walletService.getOrCreate(t.getT1(), t.getT2()))
                                .map(WalletResponse::from);
        }

        /**
         * @return liste des wallets du sub (filtré par oid si header présent)
         */
        @GetMapping
        @Operation(summary = "Lister mes portefeuilles", description = """
                Liste les portefeuilles de l'utilisateur connecté (claim sub).
                Filtrés par organisation courante (claim oid) sauf pour les admins.
                """)
        @ApiResponse(responseCode = "200", description = "Liste des portefeuilles",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = WalletResponse.class))))
        public Flux<WalletResponse> getAll() {
                return SecurityUtils.currentUserId()
                                .zipWith(SecurityUtils.currentOrganizationId())
                                .zipWith(SecurityUtils.isAdmin())
                                .flatMapMany(tuple -> {
                                        UUID userId = tuple.getT1().getT1();
                                        UUID orgId = tuple.getT1().getT2();
                                        boolean isAdmin = tuple.getT2();
                                        return walletService.findAll(userId, isAdmin ? null : orgId, isAdmin);
                                })
                                .map(WalletResponse::from);
        }

        /**
         * @param id identifiant
         * @return wallet
         */
        @GetMapping("/{id}")
        @Operation(summary = "Détail portefeuille", description = "Retourne un portefeuille après vérification d'accès (sub + oid).")
        @ApiResponse(responseCode = "200", description = "Portefeuille trouvé",
                content = @Content(schema = @Schema(implementation = WalletResponse.class)))
        public Mono<WalletResponse> getById(@PathVariable UUID id) {
                return authorizeAndMap(id);
        }

        /**
         * @param id identifiant
         * @return solde
         */
        @GetMapping("/{id}/balance")
        @Operation(summary = "Solde portefeuille", description = "Retourne le solde après vérification d'accès.")
        @ApiResponse(responseCode = "200", description = "Solde du portefeuille",
                content = @Content(schema = @Schema(implementation = WalletBalanceResponse.class)))
        public Mono<WalletBalanceResponse> getBalance(@PathVariable UUID id) {
                return SecurityUtils.currentUserId()
                                .zipWith(SecurityUtils.currentOrganizationId())
                                .zipWith(SecurityUtils.isAdmin())
                                .flatMap(t -> walletService.authorizeAccess(id, t.getT1().getT1(),
                                                t.getT1().getT2(), t.getT2()))
                                .flatMap(w -> walletService.getBalance(id).map(WalletBalanceResponse::new));
        }

        /**
         * @param request création (admin peut spécifier userId + organizationId)
         * @return wallet créé ou existant
         */
        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        @Operation(summary = "Créer un portefeuille", description = """
                Crée un portefeuille pour le couple (sub, oid) courant.
                Admin : peut spécifier `userId` et `organizationId` dans le body.
                Retourne le portefeuille existant s'il existe déjà (idempotent).
                """)
        @ApiResponse(responseCode = "201", description = "Portefeuille créé",
                content = @Content(schema = @Schema(implementation = WalletResponse.class)))
        public Mono<WalletResponse> create(@RequestBody(required = false) WalletCreateRequest request) {
                WalletCreateRequest body = request != null ? request : new WalletCreateRequest(null, null);
                return SecurityUtils.currentUserId()
                                .zipWith(SecurityUtils.currentOrganizationId())
                                .zipWith(SecurityUtils.isAdmin())
                                .flatMap(t -> {
                                        UUID userId = body.userId() != null && t.getT2()
                                                        ? body.userId()
                                                        : t.getT1().getT1();
                                        UUID orgId = body.organizationId() != null && t.getT2()
                                                        ? body.organizationId()
                                                        : t.getT1().getT2();
                                        return walletService.getOrCreate(userId, orgId);
                                })
                                .map(WalletResponse::from);
        }

        private Mono<WalletResponse> authorizeAndMap(UUID id) {
                return SecurityUtils.currentUserId()
                                .zipWith(SecurityUtils.currentOrganizationId())
                                .zipWith(SecurityUtils.isAdmin())
                                .flatMap(t -> walletService.authorizeAccess(id, t.getT1().getT1(),
                                                t.getT1().getT2(), t.getT2()))
                                .map(WalletResponse::from);
        }
}
