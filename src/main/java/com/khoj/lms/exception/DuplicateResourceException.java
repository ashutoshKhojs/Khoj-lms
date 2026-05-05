package com.khoj.lms.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends AppException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}