package com.takapedia.payment.service;

public final class FailureReasons {

    public static final String GATEWAY_UNAVAILABLE = "GATEWAY_UNAVAILABLE";
    public static final String REJECTED_BY_GATEWAY = "REJECTED_BY_GATEWAY";
    public static final String VA_EXPIRED = "VA_EXPIRED";

    private FailureReasons() {}
}