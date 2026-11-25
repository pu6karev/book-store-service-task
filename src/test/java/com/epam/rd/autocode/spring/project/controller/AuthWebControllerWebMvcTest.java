package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.ClientDTO;
import com.epam.rd.autocode.spring.project.exception.AlreadyExistException;
import com.epam.rd.autocode.spring.project.util.AbstractWebMvcTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

// Registration is public (permitAll), so no authenticated user is needed in this class.
class AuthWebControllerWebMvcTest extends AbstractWebMvcTest {
    private static final String NEW_EMAIL = "newclient@example.com";
    private static final String PASSWORD = "StrongPass1!";

    // ---------- helpers ----------

    private static MockHttpServletRequestBuilder registerRequest(String name, String email,
                                                                 String password, String confirmPassword) {
        return post("/register")
                .param("name", name)
                .param("email", email)
                .param("password", password)
                .param("confirmPassword", confirmPassword)
                .with(csrf());
    }

    // ---------- register ----------

    @Test
    void register_withValidData_shouldCreateClientWithInitialBalanceAndRedirect() throws Exception {
        when(clientService.addClient(any(ClientDTO.class))).thenReturn(new ClientDTO());

        mockMvc.perform(registerRequest("New Client", NEW_EMAIL, PASSWORD, PASSWORD))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?registered"));

        ArgumentCaptor<ClientDTO> captor = ArgumentCaptor.forClass(ClientDTO.class);
        verify(clientService).addClient(captor.capture());

        ClientDTO created = captor.getValue();
        assertThat(created.getName()).isEqualTo("New Client");
        assertThat(created.getEmail()).isEqualTo(NEW_EMAIL);
        assertThat(created.getPassword()).isEqualTo(PASSWORD);
        assertThat(created.getBalance()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(created.isBlocked()).isFalse();
    }

    @Test
    void register_whenPasswordsDoNotMatch_shouldReturnFormWithMismatchError() throws Exception {
        mockMvc.perform(registerRequest("New Client", NEW_EMAIL, PASSWORD, "DifferentPass1!"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("form", "confirmPassword"));

        verifyNoInteractions(clientService);
    }

    @Test
    void register_withInvalidFields_shouldReturnFormWithValidationErrors() throws Exception {
        mockMvc.perform(registerRequest("", "invalid-email", "weak", "weak"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("form", "name", "email", "password"));

        verifyNoInteractions(clientService);
    }

    @Test
    void register_whenEmailAlreadyExists_shouldReturnFormWithEmailError() throws Exception {
        when(clientService.addClient(any(ClientDTO.class)))
                .thenThrow(new AlreadyExistException("Email already exists"));

        mockMvc.perform(registerRequest("New Client", CLIENT_EMAIL, PASSWORD, PASSWORD))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("form", "email"));
    }
}