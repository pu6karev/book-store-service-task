package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.exception.ClientInUseException;
import com.epam.rd.autocode.spring.project.util.AbstractWebMvcTest;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

// The profile pages work for both roles, so the user is set on each test.
class ProfileWebControllerWebMvcTest extends AbstractWebMvcTest {

    // ---------- view ----------

    @Test
    @WithMockUser(username = CLIENT_EMAIL, roles = "CLIENT")
    void view_asClient_shouldRenderClientProfile() throws Exception {
        when(clientService.getClientByEmail(CLIENT_EMAIL)).thenReturn(sampleClient());

        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/view"))
                .andExpect(model().attributeExists("client"));
    }

    @Test
    @WithMockUser(username = EMPLOYEE_EMAIL, roles = "EMPLOYEE")
    void view_asEmployee_shouldRenderEmployeeProfile() throws Exception {
        when(employeeService.getEmployeeByEmail(EMPLOYEE_EMAIL)).thenReturn(sampleEmployee());

        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/view"))
                .andExpect(model().attributeExists("employee"));
    }

    // ---------- edit form ----------

    @Test
    @WithMockUser(username = CLIENT_EMAIL, roles = "CLIENT")
    void editForm_asClient_shouldPopulateForm() throws Exception {
        when(clientService.getClientByEmail(CLIENT_EMAIL)).thenReturn(sampleClient());

        mockMvc.perform(get("/profile/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/edit"))
                .andExpect(model().attributeExists("form"));
    }

    @Test
    @WithMockUser(username = EMPLOYEE_EMAIL, roles = "EMPLOYEE")
    void editForm_asEmployee_shouldPopulateForm() throws Exception {
        when(employeeService.getEmployeeByEmail(EMPLOYEE_EMAIL)).thenReturn(sampleEmployee());

        mockMvc.perform(get("/profile/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/edit"))
                .andExpect(model().attributeExists("form"));
    }

    // ---------- edit ----------

    @Test
    @WithMockUser(username = CLIENT_EMAIL, roles = "CLIENT")
    void edit_asClient_withValidName_shouldUpdateAndRedirect() throws Exception {
        mockMvc.perform(post("/profile/edit")
                        .param("name", "Updated Client")
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/profile?updated"));

        verify(clientService).updateProfile(eq(CLIENT_EMAIL), any());
    }

    @Test
    @WithMockUser(username = EMPLOYEE_EMAIL, roles = "EMPLOYEE")
    void edit_asEmployee_withValidFields_shouldUpdateAndRedirect() throws Exception {
        mockMvc.perform(post("/profile/edit")
                        .param("name", "John Doe")
                        .param("phone", "555-123-4567")
                        .param("birthDate", "1990-05-15")
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/profile?updated"));

        verify(employeeService).updateProfile(eq(EMPLOYEE_EMAIL), any());
    }

    @Test
    @WithMockUser(username = EMPLOYEE_EMAIL, roles = "EMPLOYEE")
    void edit_asEmployee_withMissingPhoneAndBirthDate_shouldReturnFormWithErrors() throws Exception {
        mockMvc.perform(post("/profile/edit")
                        .param("name", "John Doe")
                        .param("phone", "")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/edit"))
                .andExpect(model().attributeHasFieldErrors("form", "phone", "birthDate"));

        verifyNoInteractions(employeeService);
    }

    // ---------- delete account ----------

    @Test
    @WithMockUser(username = CLIENT_EMAIL, roles = "CLIENT")
    void delete_whenNoOrders_shouldDeleteAccountLogoutAndRedirect() throws Exception {
        mockMvc.perform(post("/profile/delete")
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?deleted"));

        verify(clientService).deleteClientByEmail(CLIENT_EMAIL);
    }

    @Test
    @WithMockUser(username = CLIENT_EMAIL, roles = "CLIENT")
    void delete_whenClientHasOrders_shouldRedirectToProfileWithFlashError() throws Exception {
        doThrow(new ClientInUseException("Client has orders"))
                .when(clientService).deleteClientByEmail(CLIENT_EMAIL);

        mockMvc.perform(post("/profile/delete")
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("flashError", "profile.delete.error.orders"));
    }
}