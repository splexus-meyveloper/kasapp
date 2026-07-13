package org.example.exception;

import lombok.Getter;

@Getter
public class KasappException extends RuntimeException {
    private ErrorType errorType;

    public KasappException(ErrorType errorType) {
        super(errorType.getMessage());
        this.errorType = errorType;
    }

    /** Statik enum mesajı yerine, o anki hataya özgü dinamik bir mesajla fırlatmak için. */
    public KasappException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }
}
