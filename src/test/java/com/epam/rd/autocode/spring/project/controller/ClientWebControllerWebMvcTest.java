package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.util.AbstractWebMvcTest;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static com.epam.rd.autocode.spring.project.util.AbstractWebMvcTest.EMPLOYEE_EMAIL;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Blocking of clients is an employee-only operation.
@WithMockUser(username = EMPLOYEE_EMAIL, roles = "EMPLOYEE")
class ClientWebControllerWebMvcTest extends AbstractWebMvcTest {

    @Test
    void block_shouldCallServiceAndRedirectWithFlash() throws Exception {
        mockMvc.perform(post("/clients/block")
                        .param("email", CLIENT_EMAIL)
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/clients"))
                .andExpect(flash().attribute("flashSuccess", "clients.blocked"));

        verify(clientService).setBlocked(CLIENT_EMAIL, true);
    }

    @Test
    void unblock_shouldCallServiceAndRedirectWithFlash() throws Exception {
        mockMvc.perform(post("/clients/unblock")
                        .param("email", CLIENT_EMAIL)
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/clients"))
                .andExpect(flash().attribute("flashSuccess", "clients.unblocked"));

        verify(clientService).setBlocked(CLIENT_EMAIL, false);
    }
}