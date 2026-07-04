package com.takapedia.payment.exception;

public abstract class MidtransException extends RuntimeException {
    protected MidtransException(String message, Throwable cause) {
        super(message, cause);
    }
}