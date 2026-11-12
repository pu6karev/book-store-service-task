package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.dto.BookItemDTO;
import com.epam.rd.autocode.spring.project.dto.OrderDTO;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.model.*;
import com.epam.rd.autocode.spring.project.repo.BookRepository;
import com.epam.rd.autocode.spring.project.repo.ClientRepository;
import com.epam.rd.autocode.spring.project.repo.EmployeeRepository;
import com.epam.rd.autocode.spring.project.repo.OrderRepository;
import com.epam.rd.autocode.spring.project.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public OrderDTO addOrder(OrderDTO orderDTO) {

        Client client = clientRepository.findByEmail(orderDTO.getClientEmail())
                .orElseThrow(() -> new NotFoundException("The client with email: " + orderDTO.getClientEmail() + " not found"));
        Employee employee = employeeRepository.findByEmail(orderDTO.getEmployeeEmail())
                .orElseThrow(() -> new NotFoundException("The employee with email: " + orderDTO.getEmployeeEmail() + " not found"));

        Order order = new Order();
        order.setClient(client);
        order.setEmployee(employee);
        order.setOrderDate( orderDTO.getOrderDate() == null ? LocalDateTime.now() : orderDTO.getOrderDate() );
        order.setPrice(orderDTO.getPrice());

        for (BookItemDTO itemDTO : orderDTO.getBookItems()){
            Book book = bookRepository.findByName(itemDTO.getBookName())
                    .orElseThrow(() -> new NotFoundException("Book with name: " + itemDTO.getBookName() + " not found"));

            BookItem bookItem = new BookItem();
            bookItem.setBook(book);
            bookItem.setQuantity(itemDTO.getQuantity());

            order.addBookItem(bookItem);
        }

        Order savedOrder = orderRepository.save(order);

        return convertOrderToOrderDTO(savedOrder);
    }

    private OrderDTO convertOrderToOrderDTO(Order order) {
        return mapper.map(order, OrderDTO.class);
    }
}
