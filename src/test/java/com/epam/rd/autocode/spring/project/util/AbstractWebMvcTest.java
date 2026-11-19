package com.epam.rd.autocode.spring.project.util;

import com.epam.rd.autocode.spring.project.conf.SecurityConfig;
import com.epam.rd.autocode.spring.project.dto.BookDTO;
import com.epam.rd.autocode.spring.project.dto.ClientDTO;
import com.epam.rd.autocode.spring.project.dto.EmployeeDTO;
import com.epam.rd.autocode.spring.project.model.enums.AgeGroup;
import com.epam.rd.autocode.spring.project.model.enums.Language;
import com.epam.rd.autocode.spring.project.service.BookService;
import com.epam.rd.autocode.spring.project.service.ClientService;
import com.epam.rd.autocode.spring.project.service.EmployeeService;
import com.epam.rd.autocode.spring.project.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

// Common base of all controller tests: the web layer only (controllers, advices, Thymeleaf), the services are mocks.
// A @WebMvcTest slice does not scan @Configuration classes, so the real SecurityConfig is imported:
// both filter chains, method security and CSRF work exactly as in the application.
// All subclasses have the same configuration, so Spring creates one context and reuses it.
@WebMvcTest
@Import(SecurityConfig.class)
public abstract class AbstractWebMvcTest {

    // public: they are used in class-level annotations (@WithMockUser) through a static import
    public static final String CLIENT_EMAIL = "client1@example.com";
    public static final String EMPLOYEE_EMAIL = "john.doe@email.com";
    public static final String BOOK_NAME = "Dune";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockBean
    protected BookService bookService;

    @MockBean
    protected ClientService clientService;

    @MockBean
    protected EmployeeService employeeService;

    @MockBean
    protected OrderService orderService;

    // ---------- test data ----------

    // a valid book: it passes the validation of BookDTO and can be serialized to JSON as a request body
    protected static BookDTO sampleBook(String name) {
        BookDTO book = new BookDTO();
        book.setName(name);
        book.setGenre("Sci-Fi");
        book.setAgeGroup(AgeGroup.ADULT);
        book.setPrice(new BigDecimal("29.99"));
        book.setPublicationDate(LocalDate.of(1965, 8, 1));
        book.setAuthor("Frank Herbert");
        book.setPages(412);
        book.setCharacteristics("Hardcover");
        book.setDescription("Classic sci-fi novel");
        book.setLanguage(Language.ENGLISH);
        return book;
    }

    protected static ClientDTO sampleClient() {
        return new ClientDTO("Client One", CLIENT_EMAIL, null, new BigDecimal("100.00"), false);
    }

    protected static EmployeeDTO sampleEmployee() {
        EmployeeDTO employee = new EmployeeDTO();
        employee.setName("John Doe");
        employee.setEmail(EMPLOYEE_EMAIL);
        employee.setPhone("555-123-4567");
        employee.setBirthDate(LocalDate.of(1990, 5, 15));
        return employee;
    }
}