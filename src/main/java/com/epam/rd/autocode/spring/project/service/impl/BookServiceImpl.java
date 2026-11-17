package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.dto.BookDTO;
import com.epam.rd.autocode.spring.project.exception.AlreadyExistException;
import com.epam.rd.autocode.spring.project.exception.BookInUseException;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.model.Book;
import com.epam.rd.autocode.spring.project.repo.BookRepository;
import com.epam.rd.autocode.spring.project.repo.OrderRepository;
import com.epam.rd.autocode.spring.project.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookServiceImpl implements BookService {
    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;
    private final ModelMapper mapper;

    @Override
    public List<BookDTO> getAllBooks() {
        return bookRepository.findAll().stream()
                .map(this::convertBookToBookDTO)
                .toList();
    }

    @Override
    public BookDTO getBookByName(String name) {
        Book book = bookRepository.findByName(name)
                .orElseThrow(() -> new NotFoundException("The book " + name + " not found"));

        return convertBookToBookDTO(book);
    }

    @Override
    @Transactional
    public BookDTO updateBookByName(String name, BookDTO bookDTO) {
        Book existingBook = bookRepository.findByName(name)
                .orElseThrow(() -> new NotFoundException("The book " + name + " not found"));

        // the name is the identifier: renaming is allowed only to a free name
        if (!name.equals(bookDTO.getName()) && bookRepository.existsByName(bookDTO.getName())) {
            throw new AlreadyExistException("The book with name: " + bookDTO.getName() + " already exists");
        }

        mapper.map(bookDTO, existingBook);

        Book savedBook = bookRepository.save(existingBook);
        log.debug("Book updated: {} -> {}", name, bookDTO.getName());

        return convertBookToBookDTO(savedBook);
    }

    @Override
    @Transactional
    public void deleteBookByName(String name) {
        if(!bookRepository.existsByName(name)) {
            throw new NotFoundException("The book " + name + " not found");
        }
        if(orderRepository.existsByBookItemsBookName(name)) {
            log.warn("Book {} cannot be deleted: it is included in orders", name);
            throw new BookInUseException("The book " + name + " cannot be deleted: it is included in orders");
        }

        bookRepository.deleteByName(name);
        log.info("Book deleted: {}", name);
    }

    @Override
    @Transactional
    public BookDTO addBook(BookDTO bookDTO) {
        if (bookRepository.existsByName(bookDTO.getName())) {
            throw new AlreadyExistException("The book with name: " + bookDTO.getName() + " already exists");
        }

        Book savedBook = bookRepository.save(convertBookDTOToBook(bookDTO));
        log.info("Book added: {}", bookDTO.getName());
        return convertBookToBookDTO(savedBook);
    }

    @Override
    public Page<BookDTO> searchBooks(String q, Pageable pageable) {
        log.debug("Searching books: q='{}', {}", q, pageable);
        return bookRepository.search(q, pageable).map(this::convertBookToBookDTO);
    }

    private BookDTO convertBookToBookDTO(Book book) {
        return mapper.map(book, BookDTO.class);
    }

    private Book convertBookDTOToBook(BookDTO bookDTO) {
        return mapper.map(bookDTO, Book.class);
    }
}