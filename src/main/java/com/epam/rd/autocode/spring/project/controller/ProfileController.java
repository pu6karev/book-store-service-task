package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.EmployeeDTO;
import com.epam.rd.autocode.spring.project.dto.ProfileDTO;
import com.epam.rd.autocode.spring.project.exception.ClientInUseException;
import com.epam.rd.autocode.spring.project.service.ClientService;
import com.epam.rd.autocode.spring.project.service.EmployeeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {
    private static final String FLASH_ERROR = "flashError";

    private final ClientService clientService;
    private final EmployeeService employeeService;

    @GetMapping
    public String view(Authentication authentication, Model model) {
        String email = authentication.getName();
        if (isEmployee(authentication)) {
            model.addAttribute("employee", employeeService.getEmployeeByEmail(email));
        } else {
            model.addAttribute("client", clientService.getClientByEmail(email));
        }
        return "profile/view";
    }

    @GetMapping("/edit")
    public String editForm(Authentication authentication, Model model) {
        String email = authentication.getName();
        ProfileDTO form = new ProfileDTO();
        if (isEmployee(authentication)) {
            EmployeeDTO employee = employeeService.getEmployeeByEmail(email);
            form.setName(employee.getName());
            form.setPhone(employee.getPhone());
            form.setBirthDate(employee.getBirthDate());
        } else {
            form.setName(clientService.getClientByEmail(email).getName());
        }
        model.addAttribute("form", form);
        return "profile/edit";
    }

    @PostMapping("/edit")
    public String edit(@Valid @ModelAttribute("form") ProfileDTO form,
                       BindingResult result, Authentication authentication) {
        boolean employee = isEmployee(authentication);
        if (employee) {
            if (!result.hasFieldErrors("phone")
                    && (form.getPhone() == null || form.getPhone().isBlank())) {
                result.rejectValue("phone", "validation.phone.notblank");
            }
            if (!result.hasFieldErrors("birthDate") && form.getBirthDate() == null) {
                result.rejectValue("birthDate", "validation.birthdate.notnull");
            }
        }
        if (result.hasErrors()) {
            return "profile/edit";
        }
        if (employee) {
            employeeService.updateProfile(authentication.getName(), form);
        } else {
            clientService.updateProfile(authentication.getName(), form);
        }
        return "redirect:/profile?updated";
    }

    @PostMapping("/delete")
    @PreAuthorize("hasRole('CLIENT')")
    public String delete(Authentication authentication,
                         HttpServletRequest request,
                         HttpServletResponse response,
                         RedirectAttributes redirectAttributes) {
        try {
            // only the own account: the email is taken from the authentication
            clientService.deleteClientByEmail(authentication.getName());
        } catch (ClientInUseException ex) {
            redirectAttributes.addFlashAttribute(FLASH_ERROR, "profile.delete.error.orders");
            return "redirect:/profile";
        }
        // ends the session together with the cart; that is why the message goes in the query string, not in a flash
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return "redirect:/login?deleted";
    }

    private boolean isEmployee(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));
    }
}