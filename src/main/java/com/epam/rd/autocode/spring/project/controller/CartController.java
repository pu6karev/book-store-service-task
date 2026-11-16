package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.BookDTO;
import com.epam.rd.autocode.spring.project.dto.BookItemDTO;
import com.epam.rd.autocode.spring.project.dto.OrderDTO;
import com.epam.rd.autocode.spring.project.exception.InsufficientFundsException;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.service.BookService;
import com.epam.rd.autocode.spring.project.service.ClientService;
import com.epam.rd.autocode.spring.project.service.OrderService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/cart")
@PreAuthorize("hasRole('CLIENT')")
@RequiredArgsConstructor
public class CartController {
    private static final String CART_ATTRIBUTE = "cart";
    private static final int MAX_QUANTITY = 99;
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";

    private final BookService bookService;
    private final ClientService clientService;
    private final OrderService orderService;

    @GetMapping
    public String view(HttpSession session, Authentication authentication, Model model) {
        Map<String, Integer> cart = getCart(session);
        List<BookItemDTO> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        boolean removed = false;

        for (Map.Entry<String, Integer> entry : new ArrayList<>(cart.entrySet())) {
            try {
                BookDTO book = bookService.getBookByName(entry.getKey());
                lines.add(new BookItemDTO(book.getName(), entry.getValue(), book.getPrice()));
                total = total.add(book.getPrice().multiply(BigDecimal.valueOf(entry.getValue())));
            } catch (NotFoundException ex) {
                // the book was deleted while it was lying in the cart
                cart.remove(entry.getKey());
                removed = true;
            }
        }

        if (removed && !model.containsAttribute(FLASH_ERROR)) {
            model.addAttribute(FLASH_ERROR, "cart.book.removed");
        }
        model.addAttribute("lines", lines);
        model.addAttribute("total", total);
        model.addAttribute("balance", clientService.getClientByEmail(authentication.getName()).getBalance());
        return "cart/cart";
    }

    @PostMapping("/add")
    public String add(@RequestParam("name") String name,
                      HttpSession session,
                      RedirectAttributes redirectAttributes) {
        bookService.getBookByName(name); // throws NotFoundException if there is no such book
        getCart(session).merge(name, 1, (oldQuantity, one) -> Math.min(oldQuantity + one, MAX_QUANTITY));

        redirectAttributes.addAttribute("name", name);
        redirectAttributes.addFlashAttribute(FLASH_SUCCESS, "cart.added");
        return "redirect:/books/view";
    }

    @PostMapping("/update")
    public String update(@RequestParam("name") String name,
                         @RequestParam("quantity") int quantity,
                         HttpSession session) {
        Map<String, Integer> cart = getCart(session);
        if (cart.containsKey(name)) {
            if (quantity < 1) {
                cart.remove(name);
            } else {
                cart.put(name, Math.min(quantity, MAX_QUANTITY));
            }
        }
        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String remove(@RequestParam("name") String name, HttpSession session) {
        getCart(session).remove(name);
        return "redirect:/cart";
    }

    @PostMapping("/checkout")
    public String checkout(HttpSession session,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        Map<String, Integer> cart = getCart(session);
        if (cart.isEmpty()) {
            redirectAttributes.addFlashAttribute(FLASH_ERROR, "cart.empty");
            return "redirect:/cart";
        }

        OrderDTO request = new OrderDTO();
        // the client is ALWAYS taken from the authentication, never from request parameters
        request.setClientEmail(authentication.getName());
        cart.forEach((name, quantity) -> request.getBookItems().add(new BookItemDTO(name, quantity, null)));

        try {
            orderService.addOrder(request);
        } catch (InsufficientFundsException ex) {
            redirectAttributes.addFlashAttribute(FLASH_ERROR, "cart.error.funds");
            return "redirect:/cart";
        } catch (NotFoundException ex) {
            // a book was deleted after it had been put into the cart: the cart page drops it
            redirectAttributes.addFlashAttribute(FLASH_ERROR, "cart.error.unavailable");
            return "redirect:/cart";
        }

        cart.clear();
        redirectAttributes.addFlashAttribute(FLASH_SUCCESS, "orders.placed");
        return "redirect:/orders";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> getCart(HttpSession session) {
        Map<String, Integer> cart = (Map<String, Integer>) session.getAttribute(CART_ATTRIBUTE);
        if (cart == null) {
            cart = new LinkedHashMap<>();
            session.setAttribute(CART_ATTRIBUTE, cart);
        }
        return cart;
    }
}