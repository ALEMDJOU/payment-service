package com.yowyob.payment.domain.exception;

/**
 * Email déjà utilisé.
 */
public class EmailAlreadyExistsException extends UserFriendlyException {

    /**
     * @param message détail
     */
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
