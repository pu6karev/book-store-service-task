package com.epam.rd.autocode.spring.project.conf;

import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // REST API: HTTP Basic, no session, no CSRF (there are no cookies, so there is nothing to forge)
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/clients/**", "/api/employees/**", "/api/orders/**").hasRole("EMPLOYEE")
                        .requestMatchers(HttpMethod.GET, "/api/books/**").authenticated()
                        .requestMatchers("/api/books/**").hasRole("EMPLOYEE")
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    // Web (Thymeleaf): login form, CSRF protection enabled (default).
    // Authentication is carried by the JWT in an HttpOnly cookie, the server keeps no session for it (STATELESS).
    // The HttpSession itself still exists: it holds the cart and the CSRF token.
    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http,
                                              JwtService jwtService,
                                              UserDetailsService userDetailsService) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/register", "/error", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/books/add", "/books/edit", "/books/delete").hasRole("EMPLOYEE")
                        .requestMatchers("/profile/delete").hasRole("CLIENT")
                        .requestMatchers("/cart/**", "/orders").hasRole("CLIENT")
                        .requestMatchers("/orders/**", "/clients/**").hasRole("EMPLOYEE")
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // created here and not a @Bean on purpose: a Filter bean would be registered a second time
                // in the servlet container, and it must work in the web chain only
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class)
                .formLogin(form -> form
                        .loginPage("/login")
                        // the token goes to the browser in an HttpOnly cookie; after that it is sent automatically
                        .successHandler((request, response, authentication) -> {
                            // a new login must not inherit the HttpSession of a previous user: if his token has expired
                            // and he did not log out, his cart would be handed over to the next person at this browser
                            HttpSession session = request.getSession(false);
                            if (session != null) {
                                session.invalidate();
                            }
                            response.addHeader(HttpHeaders.SET_COOKIE,
                                    jwtService.createCookie(authentication.getName()).toString());
                            response.sendRedirect(request.getContextPath() + "/books");
                        })
                        // a blocked client gets a separate message instead of "invalid email or password"
                        .failureHandler((request, response, exception) -> {
                            String target = exception instanceof LockedException ? "/login?blocked" : "/login?error";
                            response.sendRedirect(request.getContextPath() + target);
                        })
                        .permitAll())
                // the standard logout also ends the HttpSession (and with it the cart);
                // the cookie is removed by sending it back with Max-Age=0
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .deleteCookies(JwtService.COOKIE_NAME)
                        .permitAll());
        return http.build();
    }
}