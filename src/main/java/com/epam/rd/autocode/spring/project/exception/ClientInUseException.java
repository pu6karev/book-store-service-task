package com.epam.rd.autocode.spring.project.exception;

public class ClientInUseException extends RuntimeException {
    public ClientInUseException(String message) {
        super(message);
    }
}