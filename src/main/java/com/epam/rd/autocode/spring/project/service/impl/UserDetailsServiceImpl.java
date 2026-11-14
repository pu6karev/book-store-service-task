package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.repo.ClientRepository;
import com.epam.rd.autocode.spring.project.repo.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final EmployeeRepository employeeRepository;
    private final ClientRepository clientRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        return employeeRepository.findByEmail(email)
                .map(e -> build(e.getEmail(), e.getPassword(), "EMPLOYEE"))
                .or(() -> clientRepository.findByEmail(email)
                        .map(c -> build(c.getEmail(), c.getPassword(), "CLIENT")))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    private UserDetails build(String email, String password, String role) {
        return org.springframework.security.core.userdetails.User
                .withUsername(email).password(password).roles(role).build();
    }
}
