package com.epam.rd.autocode.spring.project.conf;

import com.epam.rd.autocode.spring.project.util.AbstractWebMvcTest;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Access rules of both filter chains as seen from the outside (URL rules together with @PreAuthorize):
// who gets 401/403/redirect to the login page, who gets 200, and where CSRF is required.
// The last section checks what the web chain does with the JWT cookie (JwtService and UserDetailsService are mocks).
class SecurityConfigWebMvcTest extends AbstractWebMvcTest {

    private static final String TOKEN = "any-token-value";

    @BeforeEach
    void setUpMocks() {
        // minimal non-null data so that the Thymeleaf pages render on the "200 OK" checks
        when(bookService.searchBooks(anyString(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 5), 0));
        when(clientService.getClientByEmail(anyString())).thenReturn(sampleClient());
    }

    // ---------- REST API chain (/api/**) ----------

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/books",
            "/api/clients",
            "/api/employees",
            "/api/orders/client?email=" + CLIENT_EMAIL
    })
    void anonymousApiRequests_shouldReturnUnauthorized(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = CLIENT_EMAIL, roles = "CLIENT")
    void client_shouldReadBooksApi_andBeForbiddenFromModifyingBooksApi() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/books/{name}", BOOK_NAME))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/clients",
            "/api/employees",
            "/api/orders/client?email=" + CLIENT_EMAIL
    })
    @WithMockUser(username = CLIENT_EMAIL, roles = "CLIENT")
    void client_shouldBeForbiddenFromRestrictedApiEndpoints(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/books",
            "/api/clients",
            "/api/employees",
            "/api/orders/client?email=" + CLIENT_EMAIL
    })
    @WithMockUser(username = EMPLOYEE_EMAIL, roles = "EMPLOYEE")
    void employee_shouldAccessAllGetApiEndpoints(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = EMPLOYEE_EMAIL, roles = "EMPLOYEE")
    void employee_shouldDeleteBookViaApiWithoutCsrf() throws Exception {
        // proves both EMPLOYEE access to /api/books/** and the CSRF-free stateless REST chain
        mockMvc.perform(delete("/api/books/{name}", BOOK_NAME))
                .andExpect(status().isNoContent());
    }

    // ---------- Web chain (Thymeleaf, JWT cookie, CSRF) ----------

    @ParameterizedTest
    @ValueSource(strings = {"/login", "/register"})
    void publicWebPages_shouldBeAccessibleWithoutAuthentication(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/books", "/cart", "/orders", "/clients", "/profile"})
    void anonymousWebRequests_shouldRedirectToLogin(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/books", "/cart", "/orders"})
    @WithMockUser(username = CLIENT_EMAIL, roles = "CLIENT")
    void client_shouldAccessClientWebPages(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/books/add", "/books/edit?name=" + BOOK_NAME, "/orders/pending", "/clients"})
    @WithMockUser(username = CLIENT_EMAIL, roles = "CLIENT")
    void client_shouldBeForbiddenFromEmployeeWebPages(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = CLIENT_EMAIL, roles = "CLIENT")
    void client_shouldBeForbiddenFromEmployeePostAction() throws Exception {
        mockMvc.perform(post("/books/delete")
                        .param("name", BOOK_NAME)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/books", "/books/add", "/orders/pending", "/clients"})
    @WithMockUser(username = EMPLOYEE_EMAIL, roles = "EMPLOYEE")
    void employee_shouldAccessEmployeeWebPages(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/cart", "/orders"})
    @WithMockUser(username = EMPLOYEE_EMAIL, roles = "EMPLOYEE")
    void employee_shouldBeForbiddenFromClientWebPages(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = EMPLOYEE_EMAIL, roles = "EMPLOYEE")
    void employee_shouldBeForbiddenFromClientPostActions() throws Exception {
        mockMvc.perform(post("/cart/add")
                        .param("name", BOOK_NAME)
                        .with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/profile/delete")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = CLIENT_EMAIL, roles = "CLIENT")
    void clientWebPostWithoutCsrf_shouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/cart/add")
                        .param("name", BOOK_NAME))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = EMPLOYEE_EMAIL, roles = "EMPLOYEE")
    void employeeWebPostWithoutCsrf_shouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/books/delete")
                        .param("name", BOOK_NAME))
                .andExpect(status().isForbidden());
    }

    // ---------- JWT cookie in the web chain ----------

    @Test
    void jwtCookie_whenValid_shouldAuthenticateClientWithoutSession() throws Exception {
        when(jwtService.extractEmail(TOKEN)).thenReturn(CLIENT_EMAIL);
        when(userDetailsService.loadUserByUsername(CLIENT_EMAIL)).thenReturn(clientDetails(false));

        // no @WithMockUser: the user comes only from the cookie, the roles from the loaded UserDetails
        mockMvc.perform(get("/cart").cookie(jwtCookie()))
                .andExpect(status().isOk());
    }

    @Test
    void jwtCookie_whenInvalid_shouldRedirectToLoginWithoutLoadingUser() throws Exception {
        when(jwtService.extractEmail(TOKEN)).thenThrow(new JwtException("invalid"));

        mockMvc.perform(get("/cart").cookie(jwtCookie()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("**/login"));

        verifyNoInteractions(userDetailsService);
    }

    @Test
    void jwtCookie_whenClientIsBlocked_shouldRedirectToLogin() throws Exception {
        when(jwtService.extractEmail(TOKEN)).thenReturn(CLIENT_EMAIL);
        when(userDetailsService.loadUserByUsername(CLIENT_EMAIL)).thenReturn(clientDetails(true));

        mockMvc.perform(get("/cart").cookie(jwtCookie()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void jwtCookie_whenClientIsDeleted_shouldRedirectToLogin() throws Exception {
        when(jwtService.extractEmail(TOKEN)).thenReturn(CLIENT_EMAIL);
        when(userDetailsService.loadUserByUsername(CLIENT_EMAIL))
                .thenThrow(new UsernameNotFoundException("deleted"));

        mockMvc.perform(get("/cart").cookie(jwtCookie()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // ---------- helpers ----------

    private static Cookie jwtCookie() {
        return new Cookie(JwtService.COOKIE_NAME, TOKEN);
    }

    private static UserDetails clientDetails(boolean locked) {
        return User.withUsername(CLIENT_EMAIL)
                .password("{noop}password")
                .roles("CLIENT")
                .accountLocked(locked)
                .build();
    }
}