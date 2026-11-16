package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.dto.BookItemDTO;
import com.epam.rd.autocode.spring.project.dto.OrderDTO;
import com.epam.rd.autocode.spring.project.exception.InsufficientFundsException;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.exception.OrderAlreadyConfirmedException;
import com.epam.rd.autocode.spring.project.model.Book;
import com.epam.rd.autocode.spring.project.model.BookItem;
import com.epam.rd.autocode.spring.project.model.Client;
import com.epam.rd.autocode.spring.project.model.Employee;
import com.epam.rd.autocode.spring.project.model.Order;
import com.epam.rd.autocode.spring.project.repo.BookRepository;
import com.epam.rd.autocode.spring.project.repo.ClientRepository;
import com.epam.rd.autocode.spring.project.repo.EmployeeRepository;
import com.epam.rd.autocode.spring.project.repo.OrderRepository;
import com.epam.rd.autocode.spring.project.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final BookRepository bookRepository;
    private final ModelMapper mapper;

    @Override
    public List<OrderDTO> getOrdersByClient(String clientEmail) {
        return orderRepository.findAllByClientEmail(clientEmail).stream()
                .map(this::convertOrderToOrderDTO)
                .toList();
    }

    @Override
    public List<OrderDTO> getOrdersByEmployee(String employeeEmail) {
        return orderRepository.findAllByEmployeeEmail(employeeEmail).stream()
                .map(this::convertOrderToOrderDTO)
                .toList();
    }


    @Override
    public List<OrderDTO> getPendingOrders() {
        return orderRepository.findAllByEmployeeIsNullOrderByOrderDateAsc().stream()
                .map(this::convertOrderToOrderDTO)
                .toList();
    }

    @Override
    @Transactional
    public void confirmOrder(Long orderId, String employeeEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("The order with id: " + orderId + " not found"));

        // an order is confirmed when it has an employee; the second confirmation is rejected
        if (order.getEmployee() != null) {
            throw new OrderAlreadyConfirmedException("The order with id: " + orderId + " is already confirmed");
        }

        Employee employee = employeeRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new NotFoundException("The employee with email: " + employeeEmail + " not found"));

        order.setEmployee(employee);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public OrderDTO addOrder(OrderDTO orderDTO) {
        // the client's row is locked until the end of the transaction:
        // two parallel orders cannot spend the same balance
        Client client = clientRepository.findByEmailForUpdate(orderDTO.getClientEmail())
                .orElseThrow(() -> new NotFoundException("The client with email: " + orderDTO.getClientEmail() + " not found"));

        // the employee is optional: an order without an employee is not confirmed yet
        Employee employee = null;
        String employeeEmail = orderDTO.getEmployeeEmail();
        if (employeeEmail != null && !employeeEmail.isBlank()) {
            employee = employeeRepository.findByEmail(employeeEmail)
                    .orElseThrow(() -> new NotFoundException("The employee with email: " + employeeEmail + " not found"));
        }

        Order order = new Order();
        order.setClient(client);
        order.setEmployee(employee);
        order.setOrderDate(LocalDateTime.now());

        // the price comes from the books, not from the request
        BigDecimal total = BigDecimal.ZERO;
        for (BookItemDTO itemDTO : orderDTO.getBookItems()) {
            Book book = bookRepository.findByName(itemDTO.getBookName())
                    .orElseThrow(() -> new NotFoundException("Book with name: " + itemDTO.getBookName() + " not found"));

            BookItem bookItem = new BookItem();
            bookItem.setBook(book);
            bookItem.setQuantity(itemDTO.getQuantity());
            bookItem.setUnitPrice(book.getPrice());
            order.addBookItem(bookItem);

            total = total.add(book.getPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity())));
        }

        if (client.getBalance().compareTo(total) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds: balance " + client.getBalance() + ", order total " + total);
        }
        client.setBalance(client.getBalance().subtract(total));
        clientRepository.save(client);

        order.setPrice(total);
        Order savedOrder = orderRepository.save(order);

        return convertOrderToOrderDTO(savedOrder);
    }

    private OrderDTO convertOrderToOrderDTO(Order order) {
        return mapper.map(order, OrderDTO.class);
    }
}
