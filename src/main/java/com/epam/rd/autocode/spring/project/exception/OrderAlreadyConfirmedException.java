package com.epam.rd.autocode.spring.project.exception;

public class OrderAlreadyConfirmedException extends RuntimeException {
    public OrderAlreadyConfirmedException(String message) {
        super(message);
    }
}
