package com.yowyob.payment.infrastructure.adapters.inbound.rest;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.yowyob.payment.application.ApplicationService;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.ApplicationCreateRequest;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.ApplicationResponse;

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
 * Gestion des clés API pour paiements directs.
 */
@Tag(name = "Applications", description = "Clés API paiement direct")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@OpenApiStandardResponses
public class ApplicationController {

    private final ApplicationService applicationService;

    /**
     * @param request nom application
     * @return clé API (affichée une seule fois)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer une application API")
    @ApiResponse(responseCode = "201", description = "Application créée avec clé API", content = @Content(schema = @Schema(implementation = ApplicationResponse.class)))
    public Mono<ApplicationResponse> create(@Valid @RequestBody ApplicationCreateRequest request) {
        return applicationService.createApplication(request.name()).map(this::toResponse);
    }

    /**
     * @return applications sans clés
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lister les applications")
    @ApiResponse(responseCode = "200", description = "Liste des applications", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApplicationResponse.class))))
    public Flux<ApplicationResponse> list() {
        return applicationService.listApplications().map(this::toResponse);
    }

    private ApplicationResponse toResponse(com.yowyob.payment.application.ApplicationResult result) {
        return new ApplicationResponse(result.id(), result.name(), result.apiKey(), result.active(),
                result.createdAt());
    }
}
