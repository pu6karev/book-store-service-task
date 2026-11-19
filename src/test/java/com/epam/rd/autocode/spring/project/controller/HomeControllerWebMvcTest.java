package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.util.AbstractWebMvcTest;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static com.epam.rd.autocode.spring.project.util.AbstractWebMvcTest.CLIENT_EMAIL;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser(username = CLIENT_EMAIL, roles = "CLIENT")
class HomeControllerWebMvcTest extends AbstractWebMvcTest {

    @Test
    void home_shouldRedirectToBooks() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/books"));
    }
}