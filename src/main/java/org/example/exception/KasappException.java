package org.example.exception;

import lombok.Getter;

@Getter
public class KasappException extends RuntimeException {
    private ErrorType errorType;
    public KasappException(ErrorType errorType) {
        super(errorType.getMessage());
        this.errorType = errorType;
    }
}
