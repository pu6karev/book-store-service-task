package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.dto.BookItemDTO;
import com.epam.rd.autocode.spring.project.dto.OrderDTO;
import com.epam.rd.autocode.spring.project.exception.InsufficientFundsException;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.model.Book;
import com.epam.rd.autocode.spring.project.model.BookItem;
import com.epam.rd.autocode.spring.project.model.Client;
import com.epam.rd.autocode.spring.project.model.Employee;
import com.epam.rd.autocode.spring.project.model.Order;
import com.epam.rd.autocode.spring.project.repo.BookRepository;
import com.epam.rd.autocode.spring.project.repo.ClientRepository;
import com.epam.rd.autocode.spring.project.repo.EmployeeRepository;
import com.epam.rd.autocode.spring.project.repo.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
    private static final String CLIENT_EMAIL = "client@example.com";
    private static final String EMPLOYEE_EMAIL = "employee@example.com";

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private BookRepository bookRepository;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        // the real ModelMapper is used on purpose: the mapping is part of the behaviour under test
        orderService = new OrderServiceImpl(orderRepository, clientRepository, employeeRepository,
                bookRepository, new ModelMapper());
    }

    // ---------- helpers ----------

    private static Client client(String balance) {
        return new Client(1L, "Client", CLIENT_EMAIL, "hash", new BigDecimal(balance));
    }

    private static Employee employee() {
        return new Employee(2L, "Employee", EMPLOYEE_EMAIL, "hash", "+380501234567", LocalDate.of(1990, 1, 1));
    }

    private static Book book(String name, String price) {
        Book book = new Book();
        book.setName(name);
        book.setPrice(new BigDecimal(price));
        return book;
    }

    private static BookItemDTO item(String bookName, int quantity) {
        BookItemDTO dto = new BookItemDTO();
        dto.setBookName(bookName);
        dto.setQuantity(quantity);
        return dto;
    }

    private static OrderDTO request(String employeeEmail, BookItemDTO... items) {
        OrderDTO dto = new OrderDTO();
        dto.setClientEmail(CLIENT_EMAIL);
        dto.setEmployeeEmail(employeeEmail);
        dto.setPrice(new BigDecimal("1.00")); // a price from the request must be ignored
        dto.setBookItems(new ArrayList<>(List.of(items)));
        return dto;
    }

    private static Order order(Long id, Client client, Employee employee) {
        Order order = new Order();
        order.setId(id);
        order.setClient(client);
        order.setEmployee(employee);
        order.setOrderDate(LocalDateTime.of(2026, 1, 1, 10, 0));
        order.setPrice(new BigDecimal("20.00"));

        BookItem bookItem = new BookItem();
        bookItem.setBook(book("Dune", "10.00"));
        bookItem.setQuantity(2);
        bookItem.setUnitPrice(new BigDecimal("10.00"));
        order.addBookItem(bookItem);
        return order;
    }

    private void stubSaveOrder() {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });
    }

    // ---------- addOrder ----------

    @Test
    void addOrder_withoutEmployee_calculatesPriceOnServerAndDebitsBalance() {
        Client client = client("100.00");
        when(clientRepository.findByEmailForUpdate(CLIENT_EMAIL)).thenReturn(Optional.of(client));
        when(bookRepository.findByName("Dune")).thenReturn(Optional.of(book("Dune", "10.00")));
        when(bookRepository.findByName("Emma")).thenReturn(Optional.of(book("Emma", "12.50")));
        stubSaveOrder();

        OrderDTO result = orderService.addOrder(request(null, item("Dune", 2), item("Emma", 1)));

        // 2 * 10.00 + 1 * 12.50 = 32.50; the price 1.00 from the request is ignored
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getPrice()).isEqualByComparingTo("32.50");
        assertThat(result.getClientEmail()).isEqualTo(CLIENT_EMAIL);
        assertThat(result.getEmployeeEmail()).isNull();
        assertThat(result.getBookItems()).extracting(BookItemDTO::getBookName).containsExactly("Dune", "Emma");
        assertThat(result.getBookItems()).extracting(BookItemDTO::getUnitPrice)
                .containsExactly(new BigDecimal("10.00"), new BigDecimal("12.50"));
        assertThat(client.getBalance()).isEqualByComparingTo("67.50");

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderDate()).isNotNull();
        verify(clientRepository).save(client);
        verifyNoInteractions(employeeRepository);
    }

    @Test
    void addOrder_withEmployee_setsEmployee() {
        when(clientRepository.findByEmailForUpdate(CLIENT_EMAIL)).thenReturn(Optional.of(client("100.00")));
        when(employeeRepository.findByEmail(EMPLOYEE_EMAIL)).thenReturn(Optional.of(employee()));
        when(bookRepository.findByName("Dune")).thenReturn(Optional.of(book("Dune", "10.00")));
        stubSaveOrder();

        OrderDTO result = orderService.addOrder(request(EMPLOYEE_EMAIL, item("Dune", 1)));

        assertThat(result.getEmployeeEmail()).isEqualTo(EMPLOYEE_EMAIL);
    }

    @Test
    void addOrder_blankEmployeeEmail_isTreatedAsNoEmployee() {
        when(clientRepository.findByEmailForUpdate(CLIENT_EMAIL)).thenReturn(Optional.of(client("100.00")));
        when(bookRepository.findByName("Dune")).thenReturn(Optional.of(book("Dune", "10.00")));
        stubSaveOrder();

        OrderDTO result = orderService.addOrder(request("  ", item("Dune", 1)));

        assertThat(result.getEmployeeEmail()).isNull();
        verifyNoInteractions(employeeRepository);
    }

    @Test
    void addOrder_balanceEqualsTotal_succeedsAndBalanceBecomesZero() {
        Client client = client("20.00");
        when(clientRepository.findByEmailForUpdate(CLIENT_EMAIL)).thenReturn(Optional.of(client));
        when(bookRepository.findByName("Dune")).thenReturn(Optional.of(book("Dune", "10.00")));
        stubSaveOrder();

        orderService.addOrder(request(null, item("Dune", 2)));

        assertThat(client.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void addOrder_insufficientFunds_throwsAndSavesNothing() {
        Client client = client("20.00");
        when(clientRepository.findByEmailForUpdate(CLIENT_EMAIL)).thenReturn(Optional.of(client));
        when(bookRepository.findByName("Dune")).thenReturn(Optional.of(book("Dune", "10.00")));

        assertThatThrownBy(() -> orderService.addOrder(request(null, item("Dune", 3))))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(client.getBalance()).isEqualByComparingTo("20.00");
        verify(orderRepository, never()).save(any(Order.class));
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void addOrder_clientNotFound_throws() {
        when(clientRepository.findByEmailForUpdate(CLIENT_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.addOrder(request(null, item("Dune", 1))))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(employeeRepository, bookRepository, orderRepository);
    }

    @Test
    void addOrder_employeeNotFound_throws() {
        when(clientRepository.findByEmailForUpdate(CLIENT_EMAIL)).thenReturn(Optional.of(client("100.00")));
        when(employeeRepository.findByEmail(EMPLOYEE_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.addOrder(request(EMPLOYEE_EMAIL, item("Dune", 1))))
                .isInstanceOf(NotFoundException.class);

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void addOrder_bookNotFound_throwsAndBalanceIsUntouched() {
        Client client = client("100.00");
        when(clientRepository.findByEmailForUpdate(CLIENT_EMAIL)).thenReturn(Optional.of(client));
        when(bookRepository.findByName("Unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.addOrder(request(null, item("Unknown", 1))))
                .isInstanceOf(NotFoundException.class);

        assertThat(client.getBalance()).isEqualByComparingTo("100.00");
        verify(orderRepository, never()).save(any(Order.class));
    }

    // ---------- queries ----------

    @Test
    void getOrdersByClient_mapsOrders() {
        when(orderRepository.findAllByClientEmail(CLIENT_EMAIL))
                .thenReturn(List.of(order(5L, client("0.00"), null)));

        List<OrderDTO> result = orderService.getOrdersByClient(CLIENT_EMAIL);

        assertThat(result).hasSize(1);
        OrderDTO dto = result.get(0);
        assertThat(dto.getId()).isEqualTo(5L);
        assertThat(dto.getClientEmail()).isEqualTo(CLIENT_EMAIL);
        assertThat(dto.getEmployeeEmail()).isNull();
        assertThat(dto.getBookItems()).extracting(BookItemDTO::getBookName).containsExactly("Dune");
        assertThat(dto.getBookItems()).extracting(BookItemDTO::getUnitPrice)
                .containsExactly(new BigDecimal("10.00"));
    }

    @Test
    void getOrdersByEmployee_mapsOrders() {
        when(orderRepository.findAllByEmployeeEmail(EMPLOYEE_EMAIL))
                .thenReturn(List.of(order(6L, client("0.00"), employee())));

        List<OrderDTO> result = orderService.getOrdersByEmployee(EMPLOYEE_EMAIL);

        assertThat(result).extracting(OrderDTO::getEmployeeEmail).containsExactly(EMPLOYEE_EMAIL);
    }
}