package com.takapedia.payment.exception;

/**
 * Midtrans mengembalikan HTTP 5xx, timeout, atau tidak dapat dijangkau (network error).
 * Ini masalah di sisi gateway, bukan kita.
 * MEMICU Circuit Breaker — panggilan berikutnya harus di-block sementara.
 */
public class MidtransServerException extends MidtransException {
    public MidtransServerException(String message, Throwable cause) {
        super(message, cause);
    }
}