package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.ClientDTO;
import com.epam.rd.autocode.spring.project.util.AbstractWebMvcTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.epam.rd.autocode.spring.project.util.AbstractWebMvcTest.EMPLOYEE_EMAIL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// REST API of clients (/api/clients): employee-only, see SecurityConfigWebMvcTest for the access rules.
@WithMockUser(username = EMPLOYEE_EMAIL, roles = "EMPLOYEE")
class ClientControllerWebMvcTest extends AbstractWebMvcTest {

    // ---------- helpers ----------

    // a Map is used because ClientDTO.password is WRITE_ONLY and would be omitted when serializing the DTO
    private String clientJson() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "name", "Client One",
                "email", CLIENT_EMAIL,
                "password", "StrongPass1!",
                "balance", new BigDecimal("100.00")));
    }

    // ---------- endpoints ----------

    @Test
    void getAllClients_shouldReturnList() throws Exception {
        when(clientService.getAllClients()).thenReturn(List.of(sampleClient()));

        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value(CLIENT_EMAIL));
    }

    @Test
    void getClientByEmail_shouldReturnClient() throws Exception {
        when(clientService.getClientByEmail(CLIENT_EMAIL)).thenReturn(sampleClient());

        mockMvc.perform(get("/api/clients/{email}", CLIENT_EMAIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(CLIENT_EMAIL));
    }

    @Test
    void addClient_shouldReturn201WithCreatedClient() throws Exception {
        when(clientService.addClient(any(ClientDTO.class))).thenReturn(sampleClient());

        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clientJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(CLIENT_EMAIL));
    }

    @Test
    void updateClientByEmail_shouldReturnUpdatedClient() throws Exception {
        when(clientService.updateClientByEmail(eq(CLIENT_EMAIL), any(ClientDTO.class))).thenReturn(sampleClient());

        mockMvc.perform(put("/api/clients/{email}", CLIENT_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clientJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(CLIENT_EMAIL));
    }

    @Test
    void deleteClientByEmail_shouldReturn204AndCallService() throws Exception {
        mockMvc.perform(delete("/api/clients/{email}", CLIENT_EMAIL))
                .andExpect(status().isNoContent());

        verify(clientService).deleteClientByEmail(CLIENT_EMAIL);
    }
}