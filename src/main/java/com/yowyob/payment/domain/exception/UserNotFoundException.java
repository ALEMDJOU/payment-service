package com.yowyob.payment.domain.exception;

/**
 * Utilisateur introuvable.
 */
public class UserNotFoundException extends UserFriendlyException {

    /**
     * @param message détail
     */
    public UserNotFoundException(String message) {
        super(message);
    }
}
