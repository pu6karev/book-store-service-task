package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.BookDTO;
import com.epam.rd.autocode.spring.project.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;

    @GetMapping
    public ResponseEntity<List<BookDTO>> getAllBooks() {
        List<BookDTO> books = bookService.getAllBooks();
        return ResponseEntity.ok(books);
    }

    @GetMapping("/{name}")
    public ResponseEntity<BookDTO> getBookByName(@PathVariable String name) {
        BookDTO book = bookService.getBookByName(name);
        return ResponseEntity.ok(book);
    }

    @PostMapping
    public ResponseEntity<BookDTO> addBook(@Valid @RequestBody BookDTO bookDTO) {
        BookDTO createdBook = bookService.addBook(bookDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBook);
    }

    @PutMapping("/{name}")
    public ResponseEntity<BookDTO> updateBookByName(@Valid @PathVariable String name, @RequestBody BookDTO bookDTO) {
        BookDTO updatedBook = bookService.updateBookByName(name, bookDTO);
        return ResponseEntity.ok(updatedBook);
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> deleteBookByName(@PathVariable String name) {
        bookService.deleteBookByName(name);
        return ResponseEntity.noContent().build();
    }
}