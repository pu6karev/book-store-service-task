package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.model.Client;
import com.epam.rd.autocode.spring.project.model.Employee;
import com.epam.rd.autocode.spring.project.repo.ClientRepository;
import com.epam.rd.autocode.spring.project.repo.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {
    private static final String EMAIL = "user@example.com";

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private ClientRepository clientRepository;

    private UserDetailsServiceImpl userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new UserDetailsServiceImpl(employeeRepository, clientRepository);
    }

    private static Client client(boolean blocked) {
        Client client = new Client(1L, "Client", EMAIL, "client-hash", new BigDecimal("100.00"));
        client.setBlocked(blocked);
        return client;
    }

    @Test
    void loadUserByUsername_employee_hasEmployeeRoleAndIsNotLocked() {
        Employee employee = new Employee(2L, "Employee", EMAIL, "employee-hash",
                "+380501234567", LocalDate.of(1990, 1, 1));
        when(employeeRepository.findByEmail(EMAIL)).thenReturn(Optional.of(employee));

        UserDetails result = userDetailsService.loadUserByUsername(EMAIL);

        assertThat(result.getUsername()).isEqualTo(EMAIL);
        assertThat(result.getPassword()).isEqualTo("employee-hash");
        assertThat(result.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_EMPLOYEE");
        assertThat(result.isAccountNonLocked()).isTrue();
        verifyNoInteractions(clientRepository);
    }

    @Test
    void loadUserByUsername_activeClient_hasClientRoleAndIsNotLocked() {
        when(employeeRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(clientRepository.findByEmail(EMAIL)).thenReturn(Optional.of(client(false)));

        UserDetails result = userDetailsService.loadUserByUsername(EMAIL);

        assertThat(result.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_CLIENT");
        assertThat(result.isAccountNonLocked()).isTrue();
    }

    @Test
    void loadUserByUsername_blockedClient_accountIsLocked() {
        when(employeeRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(clientRepository.findByEmail(EMAIL)).thenReturn(Optional.of(client(true)));

        UserDetails result = userDetailsService.loadUserByUsername(EMAIL);

        assertThat(result.isAccountNonLocked()).isFalse();
    }

    @Test
    void loadUserByUsername_unknownUser_throws() {
        when(employeeRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(clientRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(EMAIL))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
