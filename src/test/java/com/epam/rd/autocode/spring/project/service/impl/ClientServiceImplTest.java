package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.dto.ClientDTO;
import com.epam.rd.autocode.spring.project.exception.ClientInUseException;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.model.Client;
import com.epam.rd.autocode.spring.project.repo.ClientRepository;
import com.epam.rd.autocode.spring.project.repo.EmployeeRepository;
import com.epam.rd.autocode.spring.project.repo.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {
    private static final String EMAIL = "client@example.com";

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private ClientServiceImpl clientService;

    @BeforeEach
    void setUp() {
        // the real ModelMapper is used on purpose: the mapping is part of the behaviour under test
        clientService = new ClientServiceImpl(clientRepository, employeeRepository, orderRepository,
                new ModelMapper(), passwordEncoder);
    }

    private static Client client() {
        return new Client(1L, "Client", EMAIL, "hash", new BigDecimal("100.00"));
    }

    // ---------- setBlocked ----------

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void setBlocked_existingClient_changesFlagAndSaves(boolean blocked) {
        Client client = client();
        client.setBlocked(!blocked);
        when(clientRepository.findByEmail(EMAIL)).thenReturn(Optional.of(client));

        clientService.setBlocked(EMAIL, blocked);

        assertThat(client.isBlocked()).isEqualTo(blocked);
        verify(clientRepository).save(client);
    }

    @Test
    void setBlocked_clientNotFound_throws() {
        when(clientRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.setBlocked(EMAIL, true))
                .isInstanceOf(NotFoundException.class);

        verify(clientRepository, never()).save(any(Client.class));
    }

    // ---------- deleteClientByEmail ----------

    @Test
    void deleteClientByEmail_noOrders_deletesClient() {
        when(clientRepository.existsByEmail(EMAIL)).thenReturn(true);
        when(orderRepository.existsByClientEmail(EMAIL)).thenReturn(false);

        clientService.deleteClientByEmail(EMAIL);

        verify(clientRepository).deleteByEmail(EMAIL);
    }

    @Test
    void deleteClientByEmail_hasOrders_throwsAndDeletesNothing() {
        when(clientRepository.existsByEmail(EMAIL)).thenReturn(true);
        when(orderRepository.existsByClientEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> clientService.deleteClientByEmail(EMAIL))
                .isInstanceOf(ClientInUseException.class);

        verify(clientRepository, never()).deleteByEmail(EMAIL);
    }

    @Test
    void deleteClientByEmail_clientNotFound_throws() {
        when(clientRepository.existsByEmail(EMAIL)).thenReturn(false);

        assertThatThrownBy(() -> clientService.deleteClientByEmail(EMAIL))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(orderRepository);
        verify(clientRepository, never()).deleteByEmail(EMAIL);
    }

    // ---------- updateClientByEmail (security: a PUT must not unblock the client) ----------

    @Test
    void updateClientByEmail_blockedClient_staysBlocked() {
        Client client = client();
        client.setBlocked(true);
        when(clientRepository.findByEmail(EMAIL)).thenReturn(Optional.of(client));
        when(passwordEncoder.encode("Str0ng!Pass")).thenReturn("encoded");
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // the DTO has blocked = false by default, as after a request where the field is ignored
        ClientDTO request = new ClientDTO("New Name", EMAIL, "Str0ng!Pass", new BigDecimal("50.00"), false);

        ClientDTO result = clientService.updateClientByEmail(EMAIL, request);

        assertThat(client.getName()).isEqualTo("New Name");
        assertThat(client.getBalance()).isEqualByComparingTo("50.00");
        assertThat(client.getPassword()).isEqualTo("encoded");
        assertThat(client.isBlocked()).isTrue();
        assertThat(result.isBlocked()).isTrue();
    }
}
