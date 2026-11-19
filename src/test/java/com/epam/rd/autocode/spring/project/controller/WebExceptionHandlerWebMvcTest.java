package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.util.AbstractWebMvcTest;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static com.epam.rd.autocode.spring.project.util.AbstractWebMvcTest.CLIENT_EMAIL;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

// How the MVC handler (@ControllerAdvice) turns exceptions into error pages.
// The handler is not tied to one controller, so GET /books/view is used as the trigger.
// REST controllers are served by GlobalExceptionHandler, see GlobalExceptionHandlerWebMvcTest.
@WithMockUser(username = CLIENT_EMAIL, roles = "CLIENT")
class WebExceptionHandlerWebMvcTest extends AbstractWebMvcTest {

    @Test
    void whenNotFound_shouldRender404PageWithoutExceptionMessage() throws Exception {
        when(bookService.getBookByName("MissingBook"))
                .thenThrow(new NotFoundException("Internal missing book detail"));

        mockMvc.perform(get("/books/view").param("name", "MissingBook"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(content().string(not(containsString("Internal missing book detail"))));
    }

    @Test
    void whenUnexpectedRuntimeException_shouldRender500PageWithoutExceptionMessage() throws Exception {
        when(bookService.getBookByName("BrokenBook"))
                .thenThrow(new RuntimeException("Sensitive internal stack detail"));

        mockMvc.perform(get("/books/view").param("name", "BrokenBook"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error/500"))
                .andExpect(content().string(not(containsString("Sensitive internal stack detail"))));
    }
}