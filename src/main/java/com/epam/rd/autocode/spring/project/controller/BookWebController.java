package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.BookDTO;
import com.epam.rd.autocode.spring.project.exception.AlreadyExistException;
import com.epam.rd.autocode.spring.project.exception.BookInUseException;
import com.epam.rd.autocode.spring.project.model.enums.AgeGroup;
import com.epam.rd.autocode.spring.project.model.enums.Genre;
import com.epam.rd.autocode.spring.project.model.enums.Language;
import com.epam.rd.autocode.spring.project.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookWebController {
    private static final String FORM_VIEW = "books/form";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";

    private final BookService bookService;

    // lists for the <select> elements of the form (added to the model of every handler of this controller)
    @ModelAttribute("ageGroups")
    public AgeGroup[] ageGroups() {
        return AgeGroup.values();
    }

    @ModelAttribute("languages")
    public Language[] languages() {
        return Language.values();
    }

    @ModelAttribute("genres")
    public Genre[] genres() {
        return Genre.values();
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(required = false) Genre genre,
                       @PageableDefault(size = 5, sort = "name") Pageable pageable,
                       Model model) {
        Sort.Order order = pageable.getSort().stream().findFirst().orElse(Sort.Order.asc("name"));
        model.addAttribute("page", bookService.searchBooks(q, genre, pageable));
        model.addAttribute("q", q);
        model.addAttribute("genre", genre);
        model.addAttribute("sort", order.getProperty() + "," + order.getDirection().name().toLowerCase());
        return "books/list";
    }

    @GetMapping("/view")
    public String view(@RequestParam("name") String name, Model model) {
        model.addAttribute("book", bookService.getBookByName(name));
        return "books/details";
    }

    @GetMapping("/add")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public String addForm(Model model) {
        model.addAttribute("form", new BookDTO());
        return FORM_VIEW;
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public String add(@Valid @ModelAttribute("form") BookDTO form,
                      BindingResult result,
                      RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            bookService.addBook(form);
        } catch (AlreadyExistException ex) {
            result.rejectValue("name", "validation.book.name.exists");
            return FORM_VIEW;
        }
        redirectAttributes.addAttribute("name", form.getName());
        redirectAttributes.addFlashAttribute(FLASH_SUCCESS, "books.saved");
        return "redirect:/books/view";
    }

    @GetMapping("/edit")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public String editForm(@RequestParam("name") String name, Model model) {
        model.addAttribute("form", bookService.getBookByName(name));
        model.addAttribute("originalName", name);
        return FORM_VIEW;
    }

    @PostMapping("/edit")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public String edit(@RequestParam("originalName") String originalName,
                       @Valid @ModelAttribute("form") BookDTO form,
                       BindingResult result,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        model.addAttribute("originalName", originalName);
        if (result.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            bookService.updateBookByName(originalName, form);
        } catch (AlreadyExistException ex) {
            result.rejectValue("name", "validation.book.name.exists");
            return FORM_VIEW;
        }
        redirectAttributes.addAttribute("name", form.getName());
        redirectAttributes.addFlashAttribute(FLASH_SUCCESS, "books.saved");
        return "redirect:/books/view";
    }

    @PostMapping("/delete")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public String delete(@RequestParam("name") String name, RedirectAttributes redirectAttributes) {
        try {
            bookService.deleteBookByName(name);
        } catch (BookInUseException ex) {
            redirectAttributes.addAttribute("name", name);
            redirectAttributes.addFlashAttribute(FLASH_ERROR, "books.error.inuse");
            return "redirect:/books/view";
        }
        redirectAttributes.addFlashAttribute(FLASH_SUCCESS, "books.deleted");
        return "redirect:/books";
    }
}