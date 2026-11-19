package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.exception.OrderAlreadyConfirmedException;
import com.epam.rd.autocode.spring.project.util.AbstractWebMvcTest;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static com.epam.rd.autocode.spring.project.util.AbstractWebMvcTest.EMPLOYEE_EMAIL;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Confirmation of orders is an employee-only operation.
@WithMockUser(username = EMPLOYEE_EMAIL, roles = "EMPLOYEE")
class OrderWebControllerWebMvcTest extends AbstractWebMvcTest {
    private static final long ORDER_ID = 42L;

    @Test
    void confirmOrder_whenPending_shouldAssignEmployeeFromAuthentication() throws Exception {
        mockMvc.perform(post("/orders/{id}/confirm", ORDER_ID)
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/orders/pending"))
                .andExpect(flash().attribute("flashSuccess", "orders.confirmed"));

        // the employee comes from the authentication, never from the request
        verify(orderService).confirmOrder(ORDER_ID, EMPLOYEE_EMAIL);
    }

    @Test
    void confirmOrder_whenAlreadyConfirmed_shouldRedirectWithFlashError() throws Exception {
        doThrow(new OrderAlreadyConfirmedException("Order already confirmed"))
                .when(orderService).confirmOrder(ORDER_ID, EMPLOYEE_EMAIL);

        mockMvc.perform(post("/orders/{id}/confirm", ORDER_ID)
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/orders/pending"))
                .andExpect(flash().attribute("flashError", "orders.error.already.confirmed"));
    }
}