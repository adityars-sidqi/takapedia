package com.rahman.productservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedProductAccessException extends RuntimeException {
    public UnauthorizedProductAccessException() {
        super("You are not allowed to access product");
    }
    public UnauthorizedProductAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
