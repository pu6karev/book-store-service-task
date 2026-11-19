package com.epam.rd.autocode.spring.project.conf;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

// The JWT flow of the web chain on the real application context: real filters, real database (H2 with the demo data
// of V2), real JwtService. MockMvc does not start a server and does not keep cookies, so the tests send them by hand.
@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthenticationFlowTest {

    private static final String PASSWORD = "password123";
    private static final String CLIENT_EMAIL = "client1@example.com";
    private static final String OTHER_CLIENT_EMAIL = "client2@example.com";
    private static final String BOOK_NAME = "The Hidden Treasure";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Value("${app.jwt.secret}")
    private String secret;

    // ---------- login and logout ----------

    @Test
    void login_withValidCredentials_shouldSetHttpOnlyJwtCookieAndOpenClientPage() throws Exception {
        MvcResult result = mockMvc.perform(loginRequest(CLIENT_EMAIL))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/books"))
                .andExpect(cookie().exists(JwtService.COOKIE_NAME))
                .andExpect(cookie().httpOnly(JwtService.COOKIE_NAME, true))
                .andReturn();
        Cookie jwt = result.getResponse().getCookie(JwtService.COOKIE_NAME);

        // the next request carries only the cookie, no session: the page opens by the token alone
        mockMvc.perform(get("/cart").cookie(jwt))
                .andExpect(status().isOk())
                .andExpect(view().name("cart/cart"));
    }

    @Test
    void logout_shouldRemoveJwtCookie() throws Exception {
        Cookie jwt = loginAndGetJwt(CLIENT_EMAIL);

        mockMvc.perform(post("/logout").cookie(jwt).with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?logout"))
                .andExpect(cookie().maxAge(JwtService.COOKIE_NAME, 0));
    }

    // ---------- tokens that must be rejected ----------

    @Test
    void request_withForgedToken_shouldRedirectToLogin() throws Exception {
        // a real token whose signature is replaced
        String[] parts = loginAndGetJwt(CLIENT_EMAIL).getValue().split("\\.");

        assertCartRedirectsToLogin(parts[0] + "." + parts[1] + ".invalidsignature");
    }

    @Test
    void request_withExpiredToken_shouldRedirectToLogin() throws Exception {
        // the same secret as in the application, but a negative lifetime: the token is already expired
        assertCartRedirectsToLogin(new JwtService(secret, -1).generateToken(CLIENT_EMAIL));
    }

    @Test
    void request_withValidTokenOfUnknownUser_shouldRedirectToLogin() throws Exception {
        // the signature is correct, but there is no such user in the database (for example, the account was deleted)
        assertCartRedirectsToLogin(jwtService.generateToken("deleted.user@example.com"));
    }

    // ---------- cart and a new login ----------

    @Test
    void login_ofAnotherClientInSameBrowser_shouldNotInheritCartOfPreviousClient() throws Exception {
        Cookie firstClientJwt = loginAndGetJwt(CLIENT_EMAIL);
        // the browser keeps this HttpSession (the JSESSIONID cookie) between requests
        MockHttpSession browserSession = new MockHttpSession();

        mockMvc.perform(post("/cart/add")
                        .param("name", BOOK_NAME)
                        .cookie(firstClientJwt)
                        .session(browserSession)
                        .with(csrf()))
                .andExpect(status().isFound());
        mockMvc.perform(get("/cart").cookie(firstClientJwt).session(browserSession))
                .andExpect(model().attribute("lines", hasSize(1)));

        // the token of the first client has expired, he did not log out, and another client logs in at this browser
        MvcResult secondLogin = mockMvc.perform(loginRequest(OTHER_CLIENT_EMAIL).session(browserSession))
                .andExpect(status().isFound())
                .andReturn();

        // the old session (and the cart in it) is gone; the browser gets a new session, which is empty
        assertTrue(browserSession.isInvalid());
        Cookie secondClientJwt = secondLogin.getResponse().getCookie(JwtService.COOKIE_NAME);
        mockMvc.perform(get("/cart").cookie(secondClientJwt).session(new MockHttpSession()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("lines", empty()));
    }

    // ---------- helpers ----------

    private MockHttpServletRequestBuilder loginRequest(String email) {
        return post("/login")
                .param("username", email)
                .param("password", PASSWORD)
                .with(csrf());
    }

    private Cookie loginAndGetJwt(String email) throws Exception {
        return mockMvc.perform(loginRequest(email))
                .andReturn()
                .getResponse()
                .getCookie(JwtService.COOKIE_NAME);
    }

    private void assertCartRedirectsToLogin(String token) throws Exception {
        mockMvc.perform(get("/cart").cookie(new Cookie(JwtService.COOKIE_NAME, token)))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}