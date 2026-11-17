package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.dto.ClientDTO;
import com.epam.rd.autocode.spring.project.dto.ProfileDTO;
import com.epam.rd.autocode.spring.project.exception.AlreadyExistException;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
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
    private static final String RAW_PASSWORD = "Str0ng!Pass";
    private static final String ENCODED_PASSWORD = "encoded";

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

    // ---------- helpers ----------

    private static Client client() {
        return new Client(1L, "Client", EMAIL, "hash", new BigDecimal("100.00"));
    }

    private static ClientDTO request() {
        return new ClientDTO("New Client", EMAIL, RAW_PASSWORD, new BigDecimal("100.00"), false);
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

    // ---------- updateClientByEmail ----------

    // security: a PUT must not unblock the client
    @Test
    void updateClientByEmail_blockedClient_staysBlocked() {
        Client client = client();
        client.setBlocked(true);
        when(clientRepository.findByEmail(EMAIL)).thenReturn(Optional.of(client));
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // the DTO has blocked = false by default, as after a request where the field is ignored
        ClientDTO request = new ClientDTO("New Name", EMAIL, RAW_PASSWORD, new BigDecimal("50.00"), false);

        ClientDTO result = clientService.updateClientByEmail(EMAIL, request);

        assertThat(client.getName()).isEqualTo("New Name");
        assertThat(client.getBalance()).isEqualByComparingTo("50.00");
        assertThat(client.getPassword()).isEqualTo(ENCODED_PASSWORD);
        assertThat(client.isBlocked()).isTrue();
        assertThat(result.isBlocked()).isTrue();
    }

    @Test
    void updateClientByEmail_clientNotFound_throwsAndSavesNothing() {
        when(clientRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.updateClientByEmail(EMAIL, request()))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(passwordEncoder);
        verify(clientRepository, never()).save(any(Client.class));
    }

    // ---------- getAllClients ----------

    @Test
    void getAllClients_returnsMappedList() {
        when(clientRepository.findAll()).thenReturn(List.of(
                new Client(1L, "First", "first@example.com", "hash", new BigDecimal("10.00")),
                new Client(2L, "Second", "second@example.com", "hash", new BigDecimal("20.00"))));

        List<ClientDTO> result = clientService.getAllClients();

        assertThat(result).extracting(ClientDTO::getEmail)
                .containsExactly("first@example.com", "second@example.com");
        assertThat(result).extracting(ClientDTO::getName).containsExactly("First", "Second");
    }

    // ---------- getClientByEmail ----------

    @Test
    void getClientByEmail_found_returnsDtoWithBalanceAndBlockedFlag() {
        Client client = client();
        client.setBlocked(true);
        when(clientRepository.findByEmail(EMAIL)).thenReturn(Optional.of(client));

        ClientDTO result = clientService.getClientByEmail(EMAIL);

        assertThat(result.getName()).isEqualTo("Client");
        assertThat(result.getEmail()).isEqualTo(EMAIL);
        assertThat(result.getBalance()).isEqualByComparingTo("100.00");
        assertThat(result.isBlocked()).isTrue();
    }

    @Test
    void getClientByEmail_notFound_throws() {
        when(clientRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.getClientByEmail(EMAIL))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- addClient ----------

    @Test
    void addClient_newEmail_savesWithEncodedPassword() {
        when(clientRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(employeeRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientDTO result = clientService.addClient(request());

        ArgumentCaptor<Client> captor = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).save(captor.capture());
        Client saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo(EMAIL);
        assertThat(saved.getName()).isEqualTo("New Client");
        assertThat(saved.getBalance()).isEqualByComparingTo("100.00");
        assertThat(saved.isBlocked()).isFalse();
        // only the BCrypt hash is stored, never the raw password
        assertThat(saved.getPassword()).isEqualTo(ENCODED_PASSWORD);
        assertThat(result.getEmail()).isEqualTo(EMAIL);
    }

    @Test
    void addClient_emailTakenByClient_throwsAndSavesNothing() {
        when(clientRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> clientService.addClient(request()))
                .isInstanceOf(AlreadyExistException.class);

        verifyNoInteractions(passwordEncoder);
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void addClient_emailTakenByEmployee_throwsAndSavesNothing() {
        // security rule: an email is unique across both tables, otherwise the login would be ambiguous
        when(clientRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(employeeRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> clientService.addClient(request()))
                .isInstanceOf(AlreadyExistException.class);

        verifyNoInteractions(passwordEncoder);
        verify(clientRepository, never()).save(any(Client.class));
    }

    // ---------- updateProfile ----------

    @Test
    void updateProfile_existingClient_changesOnlyName() {
        Client existing = client();
        when(clientRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existing));
        ProfileDTO profile = new ProfileDTO();
        profile.setName("New Name");

        clientService.updateProfile(EMAIL, profile);

        assertThat(existing.getName()).isEqualTo("New Name");
        // the profile must not change the email, the password or the money
        assertThat(existing.getEmail()).isEqualTo(EMAIL);
        assertThat(existing.getPassword()).isEqualTo("hash");
        assertThat(existing.getBalance()).isEqualByComparingTo("100.00");
        verify(clientRepository).save(existing);
    }

    @Test
    void updateProfile_clientNotFound_throwsAndSavesNothing() {
        when(clientRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        ProfileDTO profile = new ProfileDTO();
        profile.setName("New Name");

        assertThatThrownBy(() -> clientService.updateProfile(EMAIL, profile))
                .isInstanceOf(NotFoundException.class);

        verify(clientRepository, never()).save(any(Client.class));
    }
}