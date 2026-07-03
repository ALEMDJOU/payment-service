package com.yowyob.payment.infrastructure.adapters.inbound.rest.dto;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Réponse d'erreur API explicite.
 */
@Schema(description = "Réponse d'erreur API")
public record ApiErrorResponse(
        @Schema(description = "Code HTTP", example = "400") int status,
        @Schema(description = "Code statut HTTP", example = "BAD_REQUEST") String code,
        @Schema(description = "Message explicite décrivant l'échec") String message,
        @Schema(description = "Erreurs de validation par champ") List<FieldErrorItem> fieldErrors,
        @Schema(description = "Horodatage ISO-8601") Instant timestamp) {

    /**
     * @param status  statut HTTP
     * @param message message explicite
     * @return réponse d'erreur
     */
    public static ApiErrorResponse of(HttpStatus status, String message) {
        return new ApiErrorResponse(status.value(), status.name(), message, List.of(), Instant.now());
    }

    /**
     * @param status      statut HTTP
     * @param message     message explicite
     * @param fieldErrors erreurs par champ
     * @return réponse d'erreur
     */
    public static ApiErrorResponse of(HttpStatus status, String message, List<FieldErrorItem> fieldErrors) {
        return new ApiErrorResponse(status.value(), status.name(), message, fieldErrors, Instant.now());
    }
}
