package com.khoj.lms.exception;

public abstract class AppException extends RuntimeException {

    public AppException(String message) {
        super(message);
    }
}