package com.rahman.productservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.math.BigDecimal;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidPriceException extends RuntimeException {
  public InvalidPriceException(BigDecimal price) {
        super("Invalid product price: " + price);
    }
  public InvalidPriceException(String message, Throwable cause) {
    super(message, cause);
  }
}
