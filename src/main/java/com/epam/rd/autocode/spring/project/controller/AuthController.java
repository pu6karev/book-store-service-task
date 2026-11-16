package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.ClientDTO;
import com.epam.rd.autocode.spring.project.dto.ClientRegistrationDTO;
import com.epam.rd.autocode.spring.project.exception.AlreadyExistException;
import com.epam.rd.autocode.spring.project.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class AuthController {
    // starting balance for a demo account; this can later be replaced by depositing funds into the account
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("100.00");

    private final ClientService clientService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new ClientRegistrationDTO());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") ClientRegistrationDTO form,
                           BindingResult result) {
        if (!result.hasFieldErrors("password") && !result.hasFieldErrors("confirmPassword")
                && !form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "validation.password.mismatch");
        }
        if (result.hasErrors()) {
            return "register";
        }
        try {
            clientService.addClient(new ClientDTO(
                    form.getName(), form.getEmail(), form.getPassword(), INITIAL_BALANCE, false));
        } catch (AlreadyExistException ex) {
            result.rejectValue("email", "validation.email.exists");
            return "register";
        }
        return "redirect:/login?registered";
    }
}
