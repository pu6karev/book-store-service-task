package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookWebController {
    private final BookService bookService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                       @PageableDefault(size = 5, sort = "name") Pageable pageable,
                       Model model) {
        model.addAttribute("page", bookService.searchBooks(q, pageable));
        model.addAttribute("q", q);
        return "books/list";
    }
}
