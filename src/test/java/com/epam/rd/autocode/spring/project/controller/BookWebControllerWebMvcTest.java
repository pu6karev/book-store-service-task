package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.BookDTO;
import com.epam.rd.autocode.spring.project.exception.AlreadyExistException;
import com.epam.rd.autocode.spring.project.exception.BookInUseException;
import com.epam.rd.autocode.spring.project.model.enums.AgeGroup;
import com.epam.rd.autocode.spring.project.model.enums.Genre;
import com.epam.rd.autocode.spring.project.model.enums.Language;
import com.epam.rd.autocode.spring.project.util.AbstractWebMvcTest;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static com.epam.rd.autocode.spring.project.util.AbstractWebMvcTest.EMPLOYEE_EMAIL;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

// The employee is the default user of this class; only the public book page is checked as a client.
@WithMockUser(username = EMPLOYEE_EMAIL, roles = "EMPLOYEE")
class BookWebControllerWebMvcTest extends AbstractWebMvcTest {

    // ---------- helpers ----------

    // a POST of the book form with valid values; only the name and the price vary between the tests
    private static MockHttpServletRequestBuilder bookFormRequest(String url, String name, String price) {
        return post(url)
                .param("name", name)
                .param("genre", Genre.SCIENCE_FICTION.name())
                .param("ageGroup", AgeGroup.ADULT.name())
                .param("price", price)
                .param("publicationDate", "1965-08-01")
                .param("author", "Frank Herbert")
                .param("pages", "412")
                .param("characteristics", "Hardcover")
                .param("description", "Classic sci-fi novel")
                .param("language", Language.ENGLISH.name())
                .with(csrf());
    }

    // ---------- list ----------

    @Test
    void list_withoutGenre_shouldPassNullGenreToServiceAndRenderGenreOptions() throws Exception {
        Pageable pageable = PageRequest.of(0, 5);
        when(bookService.searchBooks(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleBook(BOOK_NAME)), pageable, 1));

        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/list"))
                .andExpect(model().attributeExists("genres"))
                .andExpect(model().attribute("genre", nullValue()))
                // the options show the display text, not the constant name
                .andExpect(content().string(containsString("Science Fiction")));

        verify(bookService).searchBooks(eq(""), isNull(), any(Pageable.class));
    }

    @Test
    void list_withGenre_shouldPassGenreToServiceAndKeepItSelectedAndInLinks() throws Exception {
        Pageable pageable = PageRequest.of(0, 5);
        when(bookService.searchBooks(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleBook(BOOK_NAME)), pageable, 1));

        mockMvc.perform(get("/books").param("q", "du").param("genre", Genre.FANTASY.name()))
                .andExpect(status().isOk())
                .andExpect(view().name("books/list"))
                .andExpect(model().attribute("genre", Genre.FANTASY))
                // the sort links keep the genre filter
                .andExpect(content().string(containsString("genre=FANTASY")))
                // the FANTASY option is selected (the attribute order is not important)
                .andExpect(content().string(matchesPattern(
                        "(?s).*<option(?=[^>]*value=\"FANTASY\")(?=[^>]*selected)[^>]*>.*")));

        verify(bookService).searchBooks(eq("du"), eq(Genre.FANTASY), any(Pageable.class));
    }

    @Test
    void list_whenNothingFound_shouldStillRenderGenreDropDown() throws Exception {
        when(bookService.searchBooks(any(), any(), any()))
                .thenReturn(Page.empty(PageRequest.of(0, 5)));

        mockMvc.perform(get("/books").param("genre", Genre.OTHER.name()))
                .andExpect(status().isOk())
                .andExpect(view().name("books/list"))
                // // the drop-down is part of the search form, so it stays on the page when nothing is found
                .andExpect(content().string(containsString("name=\"genre\"")));
    }

    // ---------- view ----------

    @Test
    @WithMockUser(username = CLIENT_EMAIL, roles = "CLIENT")
    void view_shouldRenderBookDetails() throws Exception {
        when(bookService.getBookByName(BOOK_NAME)).thenReturn(sampleBook(BOOK_NAME));

        mockMvc.perform(get("/books/view").param("name", BOOK_NAME))
                .andExpect(status().isOk())
                .andExpect(view().name("books/details"))
                .andExpect(model().attributeExists("book"));
    }

    // ---------- add ----------

    @Test
    void add_withValidData_shouldSaveAndRedirectToDetails() throws Exception {
        when(bookService.addBook(any(BookDTO.class))).thenReturn(sampleBook(BOOK_NAME));

        mockMvc.perform(bookFormRequest("/books/add", BOOK_NAME, "29.99"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/books/view?name=" + BOOK_NAME))
                .andExpect(flash().attribute("flashSuccess", "books.saved"));

        verify(bookService).addBook(any(BookDTO.class));
    }

    @Test
    void add_withInvalidFields_shouldReturnFormWithErrorsAndSkipService() throws Exception {
        // forbidden URL characters in the name and a negative price must trigger Bean Validation
        mockMvc.perform(bookFormRequest("/books/add", "Invalid/Book?", "-10.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/form"))
                .andExpect(model().attributeHasFieldErrors("form", "name", "price"));

        verifyNoInteractions(bookService);
    }

    @Test
    void add_whenBookNameAlreadyExists_shouldReturnFormWithNameError() throws Exception {
        when(bookService.addBook(any(BookDTO.class)))
                .thenThrow(new AlreadyExistException("Book already exists"));

        mockMvc.perform(bookFormRequest("/books/add", BOOK_NAME, "29.99"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/form"))
                .andExpect(model().attributeHasFieldErrors("form", "name"));
    }

    // ---------- edit ----------

    @Test
    void editForm_shouldPopulateFormAndOriginalName() throws Exception {
        when(bookService.getBookByName(BOOK_NAME)).thenReturn(sampleBook(BOOK_NAME));

        mockMvc.perform(get("/books/edit").param("name", BOOK_NAME))
                .andExpect(status().isOk())
                .andExpect(view().name("books/form"))
                .andExpect(model().attributeExists("form"))
                .andExpect(model().attribute("originalName", BOOK_NAME));
    }

    @Test
    void edit_withValidData_shouldUpdateAndRedirectToDetails() throws Exception {
        when(bookService.updateBookByName(eq(BOOK_NAME), any(BookDTO.class)))
                .thenReturn(sampleBook("Dune Messiah"));

        mockMvc.perform(bookFormRequest("/books/edit", "Dune Messiah", "24.99")
                        .param("originalName", BOOK_NAME))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/books/view?name=Dune+Messiah"))
                .andExpect(flash().attribute("flashSuccess", "books.saved"));

        verify(bookService).updateBookByName(eq(BOOK_NAME), any(BookDTO.class));
    }

    @Test
    void edit_withInvalidFields_shouldReturnFormWithErrors() throws Exception {
        mockMvc.perform(bookFormRequest("/books/edit", "", "24.99")
                        .param("originalName", BOOK_NAME))
                .andExpect(status().isOk())
                .andExpect(view().name("books/form"))
                .andExpect(model().attribute("originalName", BOOK_NAME))
                .andExpect(model().attributeHasFieldErrors("form", "name"));

        verifyNoInteractions(bookService);
    }

    @Test
    void edit_whenTargetNameAlreadyExists_shouldReturnFormWithNameError() throws Exception {
        when(bookService.updateBookByName(eq(BOOK_NAME), any(BookDTO.class)))
                .thenThrow(new AlreadyExistException("Book already exists"));

        mockMvc.perform(bookFormRequest("/books/edit", "ExistingBook", "24.99")
                        .param("originalName", BOOK_NAME))
                .andExpect(status().isOk())
                .andExpect(view().name("books/form"))
                .andExpect(model().attribute("originalName", BOOK_NAME))
                .andExpect(model().attributeHasFieldErrors("form", "name"));
    }

    // ---------- delete ----------

    @Test
    void delete_whenUnused_shouldDeleteAndRedirectToList() throws Exception {
        mockMvc.perform(post("/books/delete")
                        .param("name", BOOK_NAME)
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/books"))
                .andExpect(flash().attribute("flashSuccess", "books.deleted"));

        verify(bookService).deleteBookByName(BOOK_NAME);
    }

    @Test
    void delete_whenBookIsUsedInOrders_shouldRedirectToDetailsWithFlashError() throws Exception {
        doThrow(new BookInUseException("Book is referenced in orders"))
                .when(bookService).deleteBookByName(BOOK_NAME);

        mockMvc.perform(post("/books/delete")
                        .param("name", BOOK_NAME)
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/books/view?name=" + BOOK_NAME))
                .andExpect(flash().attribute("flashError", "books.error.inuse"));
    }
}