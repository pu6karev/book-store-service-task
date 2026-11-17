package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.repo.ClientRepository;
import com.epam.rd.autocode.spring.project.repo.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final EmployeeRepository employeeRepository;
    private final ClientRepository clientRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        return employeeRepository.findByEmail(email)
                .map(e -> build(e.getEmail(), e.getPassword(), "EMPLOYEE", false))
                .or(() -> clientRepository.findByEmail(email)
                        .map(c -> build(c.getEmail(), c.getPassword(), "CLIENT", c.isBlocked())))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    // a locked account cannot sign in: Spring throws LockedException
    private UserDetails build(String email, String password, String role, boolean locked) {
        log.debug("User loaded: {} ({}), locked={}", email, role, locked);
        return org.springframework.security.core.userdetails.User
                .withUsername(email).password(password).roles(role).accountLocked(locked).build();
    }
}