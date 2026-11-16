package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.ClientDTO;
import com.epam.rd.autocode.spring.project.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/clients")
@PreAuthorize("hasRole('EMPLOYEE')")
@RequiredArgsConstructor
public class ClientWebController {
    private static final String FLASH_SUCCESS = "flashSuccess";

    private final ClientService clientService;

    @GetMapping
    public String list(Model model) {
        List<ClientDTO> clients = clientService.getAllClients().stream()
                .sorted(Comparator.comparing(ClientDTO::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        model.addAttribute("clients", clients);
        return "clients/list";
    }

    @PostMapping("/block")
    public String block(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
        clientService.setBlocked(email, true);
        redirectAttributes.addFlashAttribute(FLASH_SUCCESS, "clients.blocked");
        return "redirect:/clients";
    }

    @PostMapping("/unblock")
    public String unblock(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
        clientService.setBlocked(email, false);
        redirectAttributes.addFlashAttribute(FLASH_SUCCESS, "clients.unblocked");
        return "redirect:/clients";
    }
}
