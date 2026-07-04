package com.takapedia.payment.exception;

/**
 * Midtrans mengembalikan HTTP 4xx (bad request, unauthorized, dll).
 * Ini masalah di sisi kita (payload salah, credential salah), bukan gateway.
 * TIDAK MEMICU Circuit Breaker — retry tidak akan menyelesaikan masalah.
 */
public class MidtransClient4xxException extends MidtransException {
    public MidtransClient4xxException(String message, Throwable cause) {
        super(message, cause);
    }
}