package com.epam.rd.autocode.spring.project.conf;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

// Security events published by Spring Security. Passwords are never logged.
@Slf4j
@Component
public class SecurityEventListener {

    // Published once per login through the form. HTTP Basic requests of the REST API do not produce it,
    // otherwise every REST call would be logged as a login.
    @EventListener
    public void onLoginSuccess(InteractiveAuthenticationSuccessEvent event) {
        log.info("Login successful: {} {}",
                event.getAuthentication().getName(), event.getAuthentication().getAuthorities());
    }

    // Form login and HTTP Basic: wrong password, unknown user, blocked client
    @EventListener
    public void onLoginFailure(AbstractAuthenticationFailureEvent event) {
        log.warn("Login failed: {} ({})",
                event.getAuthentication().getName(), event.getException().getMessage());
    }
}