package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.OrderDTO;
import com.epam.rd.autocode.spring.project.exception.OrderAlreadyConfirmedException;
import com.epam.rd.autocode.spring.project.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderWebController {
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";

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

    @GetMapping("/pending")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public String pendingOrders(Model model) {
        model.addAttribute("orders", orderService.getPendingOrders());
        return "orders/pending";
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public String confirm(@PathVariable("id") Long id,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        try {
            // the employee is ALWAYS taken from the authentication
            orderService.confirmOrder(id, authentication.getName());
            redirectAttributes.addFlashAttribute(FLASH_SUCCESS, "orders.confirmed");
        } catch (OrderAlreadyConfirmedException ex) {
            redirectAttributes.addFlashAttribute(FLASH_ERROR, "orders.error.already.confirmed");
        }
        return "redirect:/orders/pending";
    }
}