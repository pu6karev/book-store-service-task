package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

// Thymeleaf (MVC) controllers: the answer is an error page, not plain text.
// Order 2: REST controllers always meet GlobalExceptionHandler (order 1) first.
@Slf4j
@Order(2)
@ControllerAdvice
public class WebExceptionHandler {

    // the exception message is not shown to the user
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound() {
        return "error/404";
    }

    // Not handled here: rethrown so that Spring Security answers with 403 (page error/403)
    @ExceptionHandler(AccessDeniedException.class)
    public void handleAccessDenied(AccessDeniedException ex) {
        throw ex;
    }

    // RuntimeException, not Exception: Spring's own MVC errors (unknown URL, wrong method,
    // missing parameter) are checked exceptions and keep their 404/405/400 via the Boot error pages
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleUnexpected(RuntimeException ex, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return "error/500";
    }
}