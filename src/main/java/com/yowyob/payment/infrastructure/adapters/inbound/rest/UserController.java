package com.yowyob.payment.infrastructure.adapters.inbound.rest;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yowyob.payment.application.UserService;
import com.yowyob.payment.domain.user.UserStatus;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.ChangePasswordRequest;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.MessageResponse;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.UpdateProfileRequest;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.UpdateUserStatusRequest;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.UserResponse;
import com.yowyob.payment.infrastructure.security.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Gestion du profil utilisateur.
 */
@Tag(name = "Users", description = "Profil et administration utilisateurs")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@OpenApiStandardResponses
public class UserController {

        private final UserService userService;

        /**
         * @return profil connecté
         */
        @GetMapping("/me")
        @Operation(summary = "Mon profil")
        @ApiResponse(responseCode = "200", description = "Profil utilisateur", content = @Content(schema = @Schema(implementation = UserResponse.class)))
        public Mono<UserResponse> me() {
                return SecurityUtils.currentUserId().flatMap(userService::findById).map(UserResponse::from);
        }

        /**
         * @param request nom
         * @return profil mis à jour
         */
        @PutMapping("/me")
        @Operation(summary = "Mettre à jour mon profil")
        @ApiResponse(responseCode = "200", description = "Profil mis à jour", content = @Content(schema = @Schema(implementation = UserResponse.class)))
        public Mono<UserResponse> updateMe(@Valid @RequestBody UpdateProfileRequest request) {
                return SecurityUtils.currentUserId()
                                .flatMap(id -> userService.updateProfile(id, request.name()))
                                .map(UserResponse::from);
        }

        /**
         * @param request mots de passe
         * @return confirmation
         */
        @PutMapping("/me/password")
        @Operation(summary = "Changer mon mot de passe")
        @ApiResponse(responseCode = "200", description = "Mot de passe modifié", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
        public Mono<MessageResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
                return SecurityUtils.currentUserId()
                                .flatMap(id -> userService.changePassword(id, request.oldPassword(),
                                                request.newPassword()))
                                .thenReturn(new MessageResponse("Mot de passe modifié avec succès"));
        }

        /**
         * @param id identifiant
         * @return utilisateur
         */
        @GetMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Récupérer un utilisateur (admin)")
        @ApiResponse(responseCode = "200", description = "Utilisateur trouvé", content = @Content(schema = @Schema(implementation = UserResponse.class)))
        public Mono<UserResponse> getById(@PathVariable UUID id) {
                return userService.findById(id).map(UserResponse::from);
        }

        /**
         * @param id      identifiant
         * @param request nouveau statut
         * @return utilisateur mis à jour
         */
        @PatchMapping("/{id}/status")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Changer le statut utilisateur (admin)")
        @ApiResponse(responseCode = "200", description = "Statut mis à jour", content = @Content(schema = @Schema(implementation = UserResponse.class)))
        public Mono<UserResponse> updateStatus(@PathVariable UUID id,
                        @Valid @RequestBody UpdateUserStatusRequest request) {
                return userService.updateStatus(id, UserStatus.valueOf(request.status())).map(UserResponse::from);
        }
}
