package com.twistlock.v2.exception;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static Logger log = Logger.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(value = Throwable.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorDTO handle(Throwable throwable) {
        Exception exception = (Exception) throwable;
        log.error("Exception occured ", exception);
        ErrorDTO errorDTO = new ErrorDTO();
        if (StringUtils.isNotEmpty(exception.getMessage())) {
            errorDTO.setException(exception.getMessage());
        } else {
            errorDTO.setException(exception.getMessage());
        }
        errorDTO.setErrorCode("TE001");
        return errorDTO;
    }

    @ExceptionHandler(value = RetryException.class)
    public ResponseEntity handle(RetryException retryException) {
        return new ResponseEntity<>(retryException.getBody(), retryException.getHttpStatus());
    }

    @ExceptionHandler(value = ValidationException.class)
    public ResponseEntity handle(ValidationException retryException) {
        return new ResponseEntity<>(retryException.getBody(), retryException.getHttpStatus());
    }
}
