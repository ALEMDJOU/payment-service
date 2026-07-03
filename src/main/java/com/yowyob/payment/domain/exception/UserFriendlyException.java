package com.yowyob.payment.domain.exception;

/**
 * Exception métier exposée à l'API avec message utilisateur.
 */
public class UserFriendlyException extends RuntimeException {

    /**
     * @param message message lisible
     */
    public UserFriendlyException(String message) {
        super(message);
    }
}
