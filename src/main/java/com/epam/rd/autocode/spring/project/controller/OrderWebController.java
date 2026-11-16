package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.OrderDTO;
import com.epam.rd.autocode.spring.project.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderWebController {
    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("hasRole('CLIENT')")
    public String myOrders(Authentication authentication, Model model) {
        List<OrderDTO> orders = orderService.getOrdersByClient(authentication.getName()).stream()
                .sorted(Comparator.comparing(OrderDTO::getOrderDate, Comparator.reverseOrder()))
                .toList();
        model.addAttribute("orders", orders);
        return "orders/list";
    }
}