package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.BookDTO;
import com.epam.rd.autocode.spring.project.util.AbstractWebMvcTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

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

// REST API of books (/api/books): the status codes and the JSON bodies of the endpoints.
// Who may call them is checked in SecurityConfigWebMvcTest, error responses in GlobalExceptionHandlerWebMvcTest.
@WithMockUser(username = EMPLOYEE_EMAIL, roles = "EMPLOYEE")
class BookControllerWebMvcTest extends AbstractWebMvcTest {

    @Test
    void getAllBooks_shouldReturnList() throws Exception {
        when(bookService.getAllBooks()).thenReturn(List.of(sampleBook(BOOK_NAME)));

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value(BOOK_NAME));
    }

    @Test
    void getBookByName_shouldReturnBook() throws Exception {
        when(bookService.getBookByName(BOOK_NAME)).thenReturn(sampleBook(BOOK_NAME));

        mockMvc.perform(get("/api/books/{name}", BOOK_NAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(BOOK_NAME));
    }

    @Test
    void addBook_shouldReturn201WithCreatedBook() throws Exception {
        when(bookService.addBook(any(BookDTO.class))).thenReturn(sampleBook(BOOK_NAME));

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleBook(BOOK_NAME))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(BOOK_NAME));
    }

    @Test
    void updateBookByName_shouldReturnUpdatedBook() throws Exception {
        when(bookService.updateBookByName(eq(BOOK_NAME), any(BookDTO.class))).thenReturn(sampleBook(BOOK_NAME));

        mockMvc.perform(put("/api/books/{name}", BOOK_NAME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleBook(BOOK_NAME))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(BOOK_NAME));
    }

    @Test
    void deleteBookByName_shouldReturn204AndCallService() throws Exception {
        mockMvc.perform(delete("/api/books/{name}", BOOK_NAME))
                .andExpect(status().isNoContent());

        verify(bookService).deleteBookByName(BOOK_NAME);
    }
}