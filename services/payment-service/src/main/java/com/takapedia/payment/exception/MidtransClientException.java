package com.takapedia.payment.exception;

public class MidtransClientException extends RuntimeException {
    public MidtransClientException(String message, Throwable cause) {
        super(message, cause);
    }
}