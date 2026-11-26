# BookStore

Online book store on Spring Boot: a Thymeleaf web UI for clients and employees plus a REST API for staff.
Training project (EPAM Java Fundamentals).

**Stack:** Java 17 · Spring Boot 3.2.1 · Spring Data JPA · Spring Security 6 · Thymeleaf · Flyway · H2 · jjwt 0.13.0 · Lombok · ModelMapper · JUnit 5 · Mockito · JaCoCo · Maven

## Features

| Role | What they can do |
|---|---|
| Anonymous | Register (starting balance 100.00), sign in |
| Client | Search books by title or author, filter by genre, sort and page; cart; place orders (balance is checked and charged); order history; edit profile; delete own account (only if there are no orders) |
| Employee | Add, edit, delete books; confirm pending orders; list clients and block/unblock them; edit profile |

Also: EN/UK interface switch, validation errors shown per field in the current language, 403/404/500 pages,
REST API under `/api` (`books`, `clients`, `employees`, `orders`).

## Run

Requirements: JDK 17 and Maven (there is no Maven wrapper). The application listens on port **8084**.

```bash
mvn spring-boot:run                                    # default profile: in-memory H2
mvn spring-boot:run "-Dspring-boot.run.profiles=prod"  # prod profile: file-based H2 + Flyway
```

Open <http://localhost:8084>. Demo accounts exist in both profiles; the password of all of them is `password123`.

| Role | Accounts |
|---|---|
| Employee | `john.doe@email.com`, `jane.smith@email.com` and 8 more (10 in total) |
| Client | `client1@example.com` … `client10@example.com` |

The data contains 10 books and no orders.

### Profiles

| | default | `prod` |
|---|---|---|
| Database | H2 in memory (`jdbc:h2:mem:bookdb`) | H2 file `./data/bookstore` (password `DB_PASSWORD`) |
| Schema | Hibernate `create-drop` | Flyway `V1`–`V3`, Hibernate `ddl-auto=validate` |
| Initial data | `V2__initial_data.sql`, loaded by Spring | `V2__initial_data.sql`, applied by Flyway |
| Logging | `DEBUG` for the application package, console | `INFO`, console and `logs/bookstore.log` |
| JWT secret | `app.jwt.secret` (development value) | environment variable `JWT_SECRET` |

`JWT_SECRET` must be at least 32 bytes long, otherwise the application does not start. Both `JWT_SECRET` and `DB_PASSWORD` have
demo defaults in `application-prod.properties`; set your own values outside a demo.

## Security

Two filter chains in `SecurityConfig`:

| | Web (Thymeleaf pages) | REST (`/api/**`) |
|---|---|---|
| Sign-in | form `/login` | HTTP Basic |
| Authentication carried by | JWT in an HttpOnly cookie | credentials on every request |
| Server-side session for authentication | no | no |
| CSRF protection | enabled | disabled (no cookies are used) |

- **Database-backed authentication.** `UserDetailsServiceImpl` looks the email up in `employees`, then in `clients`; the role follows from the table. Passwords are BCrypt hashes.
- **Authorization on three levels:** URL rules in the filter chains, `@PreAuthorize` on controllers, `sec:authorize` in templates.
  Employees manage books, orders and clients; clients use the cart and their own orders and profile.
  In the REST API everything except `GET /api/books` is employee-only.
- **JWT (web chain).** After a form login the server sets a signed token (HS256) in the `JWT` cookie: `HttpOnly`, `SameSite=Lax`, `Path=/`, `Max-Age` equal to the token lifetime.
  The token holds only `sub` (email), `iat` and `exp`, no roles. On every request `JwtAuthenticationFilter` verifies the token, loads the user from the database and takes the roles from there,
  so a blocked or deleted user is rejected immediately, and a forged or expired token leads to `/login`.
  Logout and account deletion clear the cookie; every login starts a new `HttpSession`, so a cart is never inherited by the next user.
- **Settings:** `app.jwt.secret`, `app.jwt.expiration-minutes` (default 60).
- **Passwords and input:** `@StrongPassword` (at least 8 characters, lower and upper case, a digit, a special character) and `@ValidEmail` on the DTOs.
  Error responses carry no stack traces or messages (`server.error.include-*=never`).

## Domain rules

- **Orders.** Prices, total and date are calculated on the server. The balance is checked and charged in one transaction under a pessimistic lock on the client row (`findByEmailForUpdate`).
  `BookItem.unitPrice` keeps the price at the time of the order. An order is confirmed when an employee is assigned to it.
- **Cart.** Held in the `HttpSession`, up to 99 copies per book.
- **Blocking.** `Client.blocked`; a blocked client cannot sign in, and a client who is already signed in is rejected on the next request.
- **Books** are identified by their unique title (`/books/view?name=`, `/api/books/{name}`); the characters `/ \ ? # % ;` are not allowed in titles.
- **Genres** are a fixed list (`Genre`: Adventure, Fantasy, Mystery, Romance, Science Fiction, Thriller, Historical Fiction, Other). The `books.genre` column stays plain text and `GenreConverter` maps it to the enum, so the migrations did not change.
- **Custom queries.** `@Query` for book search by title or author, optionally filtered by genre, with `Pageable` (the web list shows 5 per page and sorts by title, author or price) and for the locking client lookup; derived queries for the rest.

## Structure

`com.epam.rd.autocode.spring.project`: `conf` (security, JWT, locale, ModelMapper) · `controller` (REST and MVC controllers, MVC exception handler) · `dto` · `exception` (six custom exceptions, REST handler) · `model` · `repo` · `service` and `service.impl` · `validation`.
Controllers that render pages are named `*WebController`; REST controllers are named `*Controller`.
Templates are in `src/main/resources/templates`, messages in `messages.properties` and `messages_uk.properties`, migrations in `db/migration`.

- **Exceptions.** `GlobalExceptionHandler` serves the REST controllers (404, 409, generic 500 as plain text); `WebExceptionHandler` serves the MVC controllers (`error/404`, `error/500`); 403 comes from Spring Security (`error/403`).
- **Logging.** Explicit `@Slf4j` logging in services, security listener and JWT filter. Creation, deletion, blocking and confirmation are `INFO`; reads and updates are `DEBUG`; rejected operations and failed logins are `WARN`; unhandled exceptions are `ERROR`.
  Passwords and tokens are never logged.

## Tests

```bash
mvn test
```

351 tests, all green. The JaCoCo report is generated by the same command: `target/site/jacoco/index.html`.

| Layer | Tests |
|---|---|
| Services (Mockito) | 60 |
| Controllers and security chains (`@WebMvcTest`, real `SecurityConfig`) | 108 |
| JWT (unit tests plus a `@SpringBootTest` login flow) | 15 |
| DTOs, models, exceptions, configuration, context startup | 168 |

Coverage from the JaCoCo report (Lombok-generated code is excluded via `lombok.config`):

| | Instructions | Branches |
|---|---|---|
| Whole project | 98.2% | 88.9% |
| `service.impl` | 99.4% | 100% |
| `controller` | 99.8% | 87.0% |
| `conf` | 94.9% | 66.7% |

## Quick tour

1. Sign in as `client1@example.com`, open a book, *Add to cart*, then *Cart* → *Place order*.
2. In a private window sign in as `john.doe@email.com`: *Pending orders* → *Confirm*; *Clients* → *Block* the client.
3. The blocked client's next click leads to `/login`: the user is checked in the database on every request.
4. In *Books* type part of a title and pick a genre in the list next to the search box: both filters work together.
5. Switch `EN | UK` in the header; open `/clients` as a client to see the 403 page.
6. REST: `curl -u john.doe@email.com:password123 http://localhost:8084/api/clients`

## Known limitations

- Only authentication is stateless: the cart and the CSRF token live in the `HttpSession`.
- A JWT cannot be revoked before it expires (mitigated by the 60-minute lifetime and the database check on every request).
- The cookie has no `Secure` flag (the application runs on plain HTTP); the default secrets and the demo accounts are for demonstration only, including the `prod` profile.
- A client with orders cannot delete the account. There is no password change or reset, login throttling, OAuth2 or refresh tokens. A client's balance can only be set by an employee through the REST API.
- Three known paths end in a 500 page or response: an unknown `sort` property in `/books?sort=`, an unknown `genre` value in `/books?genre=`, and a REST `PUT` that sets an email already in use.
- Validation messages in the REST DTOs and the exception texts in services are English only.