package com.twistlock.v2.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class RetryException extends RuntimeException {

    private String body;

    private HttpStatus httpStatus;

    public RetryException(String message) {
        super(message);
    }

    public RetryException(String body, HttpStatus httpStatus) {
        this.body = body;
        this.httpStatus = httpStatus;
    }
}
