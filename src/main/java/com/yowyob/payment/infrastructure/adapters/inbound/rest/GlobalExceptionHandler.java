package com.yowyob.payment.infrastructure.adapters.inbound.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import com.yowyob.payment.domain.exception.InsufficientBalanceException;
import com.yowyob.payment.domain.exception.TransactionNotFoundException;
import com.yowyob.payment.domain.exception.UnsupportedPaymentMethodException;
import com.yowyob.payment.domain.exception.UserFriendlyException;
import com.yowyob.payment.domain.exception.WalletNotFoundException;
import com.yowyob.payment.domain.transaction.exception.InvalidTransitionException;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.ApiErrorResponse;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.FieldErrorItem;

import reactor.core.publisher.Mono;

/**
 * Gestion centralisée des erreurs API avec messages explicites.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * @param ex exception métier
     * @return 400
     */
    @ExceptionHandler(UserFriendlyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ApiErrorResponse> handleUserFriendly(UserFriendlyException ex) {
        return Mono.just(ApiErrorResponse.of(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    /**
     * @param ex portefeuille introuvable
     * @return 404
     */
    @ExceptionHandler(WalletNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<ApiErrorResponse> handleWalletNotFound(WalletNotFoundException ex) {
        return Mono.just(ApiErrorResponse.of(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    /**
     * @param ex transaction introuvable
     * @return 404
     */
    @ExceptionHandler(TransactionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<ApiErrorResponse> handleTransactionNotFound(TransactionNotFoundException ex) {
        return Mono.just(ApiErrorResponse.of(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    /**
     * @param ex solde insuffisant
     * @return 400
     */
    @ExceptionHandler(InsufficientBalanceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ApiErrorResponse> handleInsufficientBalance(InsufficientBalanceException ex) {
        return Mono.just(ApiErrorResponse.of(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    /**
     * @param ex méthode de paiement non supportée
     * @return 422
     */
    @ExceptionHandler(UnsupportedPaymentMethodException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Mono<ApiErrorResponse> handleUnsupportedPaymentMethod(UnsupportedPaymentMethodException ex) {
        return Mono.just(ApiErrorResponse.of(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage()));
    }

    /**
     * @param ex transition FSM illégale
     * @return 422
     */
    @ExceptionHandler(InvalidTransitionException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Mono<ApiErrorResponse> handleInvalidTransition(InvalidTransitionException ex) {
        return Mono.just(ApiErrorResponse.of(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage()));
    }

    /**
     * @param ex argument invalide
     * @return 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return Mono.just(ApiErrorResponse.of(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    /**
     * @param ex accès refusé
     * @return 403
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Mono<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        String message = ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : "Accès refusé : permissions insuffisantes pour cette ressource";
        return Mono.just(ApiErrorResponse.of(HttpStatus.FORBIDDEN, message));
    }

    /**
     * @param ex authentification échouée
     * @return 401
     */
    @ExceptionHandler({ AuthenticationException.class, BadCredentialsException.class })
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Mono<ApiErrorResponse> handleAuthentication(AuthenticationException ex) {
        String message = ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : "Authentification échouée : identifiants invalides";
        return Mono.just(ApiErrorResponse.of(HttpStatus.UNAUTHORIZED, message));
    }

    /**
     * @param ex validation Bean Validation
     * @return 400
     */
    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ApiErrorResponse> handleValidation(WebExchangeBindException ex) {
        List<FieldErrorItem> fieldErrors = new ArrayList<>();
        ex.getFieldErrors().forEach(error -> fieldErrors.add(
                new FieldErrorItem(error.getField(), error.getDefaultMessage())));
        ex.getGlobalErrors().forEach(error -> fieldErrors.add(
                new FieldErrorItem(error.getObjectName(), error.getDefaultMessage())));
        String message = fieldErrors.stream()
                .map(item -> item.field() + " : " + item.message())
                .collect(Collectors.joining(" ; "));
        if (message.isBlank()) {
            message = "Validation échouée : un ou plusieurs champs sont invalides";
        }
        return Mono.just(ApiErrorResponse.of(HttpStatus.BAD_REQUEST, message, fieldErrors));
    }

    /**
     * @param ex erreur non mappée
     * @return 500
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ApiErrorResponse> handleUnexpected(Exception ex) {
        log.error("Erreur inattendue", ex);
        String detail = ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : "aucun détail disponible";
        String message = "Erreur inattendue : " + ex.getClass().getSimpleName() + " - " + detail;
        return Mono.just(ApiErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, message));
    }
}
