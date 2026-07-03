package com.yowyob.payment.infrastructure.adapters.inbound.rest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.yowyob.payment.application.AuthService;
import com.yowyob.payment.application.UserService;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.AuthResponse;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.LoginRequest;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.RegisterRequest;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.UserResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Endpoints d'authentification publics.
 */
@Tag(name = "Auth", description = "Inscription et connexion JWT")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@OpenApiStandardResponses
public class AuthController {

        private final UserService userService;
        private final AuthService authService;

        /**
         * @param request inscription
         * @return utilisateur créé
         */
        @PostMapping("/register")
        @ResponseStatus(HttpStatus.CREATED)
        @Operation(summary = "Inscription", description = "Crée un compte utilisateur avec wallet par défaut")
        @ApiResponse(responseCode = "201", description = "Utilisateur créé", content = @Content(schema = @Schema(implementation = UserResponse.class)))
        public Mono<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
                return userService.register(request.name(), request.email(), request.password())
                                .map(UserResponse::from);
        }

        /**
         * @param request connexion
         * @return token JWT
         */
        @PostMapping("/login")
        @Operation(summary = "Connexion", description = "Retourne un JWT stateless")
        @ApiResponse(responseCode = "200", description = "Authentification réussie", content = @Content(schema = @Schema(implementation = AuthResponse.class)))
        public Mono<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
                return authService.login(request.email(), request.password())
                                .map(map -> new AuthResponse((String) map.get("token"), (Long) map.get("expiresIn")));
        }
}
