package com.epam.rd.autocode.spring.project.exception;

import com.epam.rd.autocode.spring.project.dto.BookDTO;
import com.epam.rd.autocode.spring.project.dto.BookItemDTO;
import com.epam.rd.autocode.spring.project.dto.OrderDTO;
import com.epam.rd.autocode.spring.project.util.AbstractWebMvcTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static com.epam.rd.autocode.spring.project.util.AbstractWebMvcTest.EMPLOYEE_EMAIL;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// How the REST handler (@RestControllerAdvice) turns exceptions into status codes and bodies.
// MVC controllers are served by WebExceptionHandler, see WebExceptionHandlerWebMvcTest.
@WithMockUser(username = EMPLOYEE_EMAIL, roles = "EMPLOYEE")
class GlobalExceptionHandlerWebMvcTest extends AbstractWebMvcTest {

    @Test
    void whenNotFound_shouldReturn404WithMessage() throws Exception {
        when(bookService.getBookByName("MissingBook"))
                .thenThrow(new NotFoundException("Book not found"));

        mockMvc.perform(get("/api/books/{name}", "MissingBook"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Book not found"));
    }

    @Test
    void whenAlreadyExists_shouldReturn409WithMessage() throws Exception {
        when(bookService.addBook(any(BookDTO.class)))
                .thenThrow(new AlreadyExistException("Book already exists"));

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleBook(BOOK_NAME))))
                .andExpect(status().isConflict())
                .andExpect(content().string("Book already exists"));
    }

    @Test
    void whenBookInUse_shouldReturn409WithMessage() throws Exception {
        doThrow(new BookInUseException("Book is used in orders"))
                .when(bookService).deleteBookByName(BOOK_NAME);

        mockMvc.perform(delete("/api/books/{name}", BOOK_NAME))
                .andExpect(status().isConflict())
                .andExpect(content().string("Book is used in orders"));
    }

    @Test
    void whenClientInUse_shouldReturn409WithMessage() throws Exception {
        doThrow(new ClientInUseException("Client has existing orders"))
                .when(clientService).deleteClientByEmail(CLIENT_EMAIL);

        mockMvc.perform(delete("/api/clients/{email}", CLIENT_EMAIL))
                .andExpect(status().isConflict())
                .andExpect(content().string("Client has existing orders"));
    }

    @Test
    void whenInsufficientFunds_shouldReturn409WithMessage() throws Exception {
        when(orderService.addOrder(any(OrderDTO.class)))
                .thenThrow(new InsufficientFundsException("Insufficient funds"));

        OrderDTO order = new OrderDTO();
        order.setClientEmail(CLIENT_EMAIL);
        order.setBookItems(List.of(new BookItemDTO(BOOK_NAME, 2, null)));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Insufficient funds"));
    }

    @Test
    void whenBodyFailsValidation_shouldReturn400() throws Exception {
        // an empty payload breaks the @NotBlank / @NotNull rules of BookDTO
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whenJsonIsMalformed_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whenUnexpectedError_shouldReturn500WithoutInternalDetails() throws Exception {
        when(bookService.getAllBooks())
                .thenThrow(new RuntimeException("Sensitive DB connection string"));

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Internal server error"))
                .andExpect(content().string(not(containsString("Sensitive DB"))));
    }
}