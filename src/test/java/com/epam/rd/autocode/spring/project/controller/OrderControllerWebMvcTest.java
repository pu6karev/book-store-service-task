package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.BookItemDTO;
import com.epam.rd.autocode.spring.project.dto.OrderDTO;
import com.epam.rd.autocode.spring.project.util.AbstractWebMvcTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.epam.rd.autocode.spring.project.util.AbstractWebMvcTest.EMPLOYEE_EMAIL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// REST API of orders (/api/orders): employee-only, see SecurityConfigWebMvcTest for the access rules.
// The business rules of placing an order (prices, balance) belong to OrderServiceImplTest.
@WithMockUser(username = EMPLOYEE_EMAIL, roles = "EMPLOYEE")
class OrderControllerWebMvcTest extends AbstractWebMvcTest {
    private static final long ORDER_ID = 10L;

    // ---------- helpers ----------

    private static OrderDTO sampleOrder() {
        return new OrderDTO(
                ORDER_ID,
                CLIENT_EMAIL,
                EMPLOYEE_EMAIL,
                LocalDateTime.of(2026, 11, 15, 12, 0),
                new BigDecimal("59.98"),
                List.of(new BookItemDTO(BOOK_NAME, 2, new BigDecimal("29.99"))));
    }

    // ---------- endpoints ----------

    @Test
    void getOrdersByClient_shouldReturnList() throws Exception {
        when(orderService.getOrdersByClient(CLIENT_EMAIL)).thenReturn(List.of(sampleOrder()));

        mockMvc.perform(get("/api/orders/client").param("email", CLIENT_EMAIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ORDER_ID));
    }

    @Test
    void getOrdersByEmployee_shouldReturnList() throws Exception {
        when(orderService.getOrdersByEmployee(EMPLOYEE_EMAIL)).thenReturn(List.of(sampleOrder()));

        mockMvc.perform(get("/api/orders/employee").param("email", EMPLOYEE_EMAIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ORDER_ID));
    }

    @Test
    void addOrder_shouldReturn201WithCreatedOrder() throws Exception {
        when(orderService.addOrder(any(OrderDTO.class))).thenReturn(sampleOrder());

        OrderDTO request = new OrderDTO();
        request.setClientEmail(CLIENT_EMAIL);
        request.setBookItems(List.of(new BookItemDTO(BOOK_NAME, 2, null)));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ORDER_ID));
    }
}