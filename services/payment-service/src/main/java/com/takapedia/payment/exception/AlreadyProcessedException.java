package com.takapedia.payment.exception;

import java.util.UUID;

public class AlreadyProcessedException extends RuntimeException {
    public AlreadyProcessedException(UUID orderId) {
        super("Event for orderId " + orderId + " already processed");
    }
}