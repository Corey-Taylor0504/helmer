package com.twistlock.v2.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class ValidationException extends RuntimeException {

    private String body;

    private HttpStatus httpStatus;

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String body, HttpStatus httpStatus) {
        this.body = body;
        this.httpStatus = httpStatus;
    }
}
