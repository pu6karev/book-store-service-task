package com.epam.rd.autocode.spring.project.conf;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;

// Authenticates a web request by the JWT cookie. Not a @Component on purpose: it is created in SecurityConfig
// and added to the web chain only (a Filter bean would also be registered in the servlet container).
// The user is loaded from the database on every request, so the roles are always current and a blocked or
// deleted user is rejected at once. An invalid token is simply ignored: the request stays anonymous
// and Spring Security redirects to /login.
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    // The web chain is STATELESS, so its context lives in a request attribute. The context must be saved there
    // explicitly: the error pages (403, 404) are rendered in a separate "error dispatch" that reads the user from it.
    private final SecurityContextRepository securityContextRepository = new RequestAttributeSecurityContextRepository();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Cookie cookie = WebUtils.getCookie(request, JwtService.COOKIE_NAME);
        // no cookie: nothing to do; already authenticated (for example a test user): do not overwrite
        if (cookie != null && StringUtils.hasText(cookie.getValue())
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(cookie.getValue(), request, response);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request, HttpServletResponse response) {
        try {
            String email = jwtService.extractEmail(token);
            UserDetails user = userDetailsService.loadUserByUsername(email);
            if (!user.isAccountNonLocked()) {
                log.warn("JWT rejected: account {} is blocked", email);
                return;
            }
            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);
        } catch (ExpiredJwtException ex) {
            log.debug("JWT expired");
        } catch (JwtException | IllegalArgumentException ex) {
            // only the exception type is logged: neither the token nor its content ends up in the log
            log.warn("Invalid JWT rejected: {}", ex.getClass().getSimpleName());
        } catch (UsernameNotFoundException ex) {
            log.warn("JWT rejected: user does not exist any more");
        }
    }
}