package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.BookDTO;
import com.epam.rd.autocode.spring.project.dto.OrderDTO;
import com.epam.rd.autocode.spring.project.exception.InsufficientFundsException;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.util.AbstractWebMvcTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.epam.rd.autocode.spring.project.util.AbstractWebMvcTest.CLIENT_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

// The cart lives in the HttpSession (attribute "cart": book name -> quantity), only for clients.
@WithMockUser(username = CLIENT_EMAIL, roles = "CLIENT")
class CartControllerWebMvcTest extends AbstractWebMvcTest {
    private static final String DELETED_BOOK = "DeletedBook";

    // ---------- helpers ----------

    // the controller changes the cart in place, so the map must be mutable (Map.of would not do)
    private static MockHttpSession sessionWithCart(String bookName, int quantity) {
        MockHttpSession session = new MockHttpSession();
        Map<String, Integer> cart = new LinkedHashMap<>();
        cart.put(bookName, quantity);
        session.setAttribute("cart", cart);
        return session;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Integer> cartOf(MockHttpSession session) {
        return (Map<String, Integer>) session.getAttribute("cart");
    }

    // ---------- checkout ----------

    @Test
    void checkout_shouldUseAuthenticatedEmailClearCartAndRedirectToOrders() throws Exception {
        MockHttpSession session = sessionWithCart(BOOK_NAME, 2);
        when(orderService.addOrder(any(OrderDTO.class))).thenReturn(new OrderDTO());

        mockMvc.perform(post("/cart/checkout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/orders"))
                .andExpect(flash().attribute("flashSuccess", "orders.placed"));

        ArgumentCaptor<OrderDTO> captor = ArgumentCaptor.forClass(OrderDTO.class);
        verify(orderService).addOrder(captor.capture());

        // the client comes from the authentication and the price is left for the server to set
        OrderDTO sentOrder = captor.getValue();
        assertThat(sentOrder.getClientEmail()).isEqualTo(CLIENT_EMAIL);
        assertThat(sentOrder.getBookItems()).hasSize(1);
        assertThat(sentOrder.getBookItems().get(0).getBookName()).isEqualTo(BOOK_NAME);
        assertThat(sentOrder.getBookItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(sentOrder.getBookItems().get(0).getUnitPrice()).isNull();

        assertThat(cartOf(session)).isEmpty();
    }

    @Test
    void checkout_whenCartIsEmpty_shouldRedirectWithEmptyError() throws Exception {
        mockMvc.perform(post("/cart/checkout")
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("flashError", "cart.empty"));

        verifyNoInteractions(orderService);
    }

    @Test
    void checkout_whenInsufficientFunds_shouldPreserveCartAndSetFlashError() throws Exception {
        MockHttpSession session = sessionWithCart(BOOK_NAME, 3);
        when(orderService.addOrder(any(OrderDTO.class)))
                .thenThrow(new InsufficientFundsException("Insufficient funds"));

        mockMvc.perform(post("/cart/checkout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("flashError", "cart.error.funds"));

        assertThat(cartOf(session)).containsEntry(BOOK_NAME, 3);
    }

    @Test
    void checkout_whenBookDeletedBeforeCheckout_shouldRedirectWithUnavailableError() throws Exception {
        MockHttpSession session = sessionWithCart(DELETED_BOOK, 1);
        when(orderService.addOrder(any(OrderDTO.class)))
                .thenThrow(new NotFoundException("Book not found"));

        mockMvc.perform(post("/cart/checkout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("flashError", "cart.error.unavailable"));
    }

    // ---------- view ----------

    @Test
    void view_whenBookDeletedFromCatalog_shouldRemoveItFromSessionAndShowError() throws Exception {
        MockHttpSession session = sessionWithCart(DELETED_BOOK, 1);
        cartOf(session).put(BOOK_NAME, 2);

        BookDTO dune = sampleBook(BOOK_NAME);
        dune.setPrice(new BigDecimal("25.00"));

        when(bookService.getBookByName(DELETED_BOOK)).thenThrow(new NotFoundException("Book not found"));
        when(bookService.getBookByName(BOOK_NAME)).thenReturn(dune);
        when(clientService.getClientByEmail(CLIENT_EMAIL)).thenReturn(sampleClient());

        mockMvc.perform(get("/cart").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("cart/cart"))
                .andExpect(model().attribute("flashError", "cart.book.removed"))
                .andExpect(model().attribute("total", new BigDecimal("50.00")));

        assertThat(cartOf(session)).containsOnlyKeys(BOOK_NAME);
    }

    // ---------- add, update, remove ----------

    @Test
    void add_shouldCapItemQuantityAt99() throws Exception {
        MockHttpSession session = sessionWithCart(BOOK_NAME, 99);
        when(bookService.getBookByName(BOOK_NAME)).thenReturn(sampleBook(BOOK_NAME));

        mockMvc.perform(post("/cart/add")
                        .param("name", BOOK_NAME)
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/books/view?name=" + BOOK_NAME));

        assertThat(cartOf(session)).containsEntry(BOOK_NAME, 99);
    }

    @Test
    void update_shouldCapAt99AndRemoveWhenLessThanOne() throws Exception {
        MockHttpSession session = sessionWithCart(BOOK_NAME, 2);
        cartOf(session).put("Foundation", 1);

        mockMvc.perform(post("/cart/update")
                        .param("name", BOOK_NAME)
                        .param("quantity", "150")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/cart"));

        mockMvc.perform(post("/cart/update")
                        .param("name", "Foundation")
                        .param("quantity", "0")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/cart"));

        assertThat(cartOf(session)).containsEntry(BOOK_NAME, 99).doesNotContainKey("Foundation");
    }

    @Test
    void remove_shouldDeleteItemFromSessionCart() throws Exception {
        MockHttpSession session = sessionWithCart(BOOK_NAME, 2);

        mockMvc.perform(post("/cart/remove")
                        .param("name", BOOK_NAME)
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/cart"));

        assertThat(cartOf(session)).isEmpty();
    }
}