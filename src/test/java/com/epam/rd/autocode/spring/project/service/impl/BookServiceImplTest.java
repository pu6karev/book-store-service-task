package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.dto.BookDTO;
import com.epam.rd.autocode.spring.project.exception.AlreadyExistException;
import com.epam.rd.autocode.spring.project.exception.BookInUseException;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.model.Book;
import com.epam.rd.autocode.spring.project.model.enums.AgeGroup;
import com.epam.rd.autocode.spring.project.model.enums.Language;
import com.epam.rd.autocode.spring.project.repo.BookRepository;
import com.epam.rd.autocode.spring.project.repo.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private OrderRepository orderRepository;

    private BookServiceImpl bookService;

    @BeforeEach
    void setUp() {
        // the real ModelMapper is used on purpose: the mapping is part of the behaviour under test
        bookService = new BookServiceImpl(bookRepository, orderRepository, new ModelMapper());
    }

    private static Book book(String name, String price) {
        Book book = new Book();
        book.setId(1L);
        book.setName(name);
        book.setGenre("Fantasy");
        book.setAgeGroup(AgeGroup.ADULT);
        book.setPrice(new BigDecimal(price));
        book.setPublicationDate(LocalDate.of(2020, 1, 1));
        book.setAuthor("Author");
        book.setPages(100);
        book.setLanguage(Language.ENGLISH);
        return book;
    }

    private static BookDTO bookDto(String name, String price) {
        BookDTO dto = new BookDTO();
        dto.setName(name);
        dto.setGenre("Fantasy");
        dto.setAgeGroup(AgeGroup.ADULT);
        dto.setPrice(new BigDecimal(price));
        dto.setPublicationDate(LocalDate.of(2020, 1, 1));
        dto.setAuthor("Author");
        dto.setPages(100);
        dto.setLanguage(Language.ENGLISH);
        return dto;
    }

    @Test
    void getAllBooks_returnsMappedList() {
        when(bookRepository.findAll()).thenReturn(List.of(book("Dune", "10.00"), book("Emma", "12.00")));

        List<BookDTO> result = bookService.getAllBooks();

        assertThat(result).extracting(BookDTO::getName).containsExactly("Dune", "Emma");
    }

    @Test
    void getBookByName_found_returnsDto() {
        when(bookRepository.findByName("Dune")).thenReturn(Optional.of(book("Dune", "10.00")));

        BookDTO result = bookService.getBookByName("Dune");

        assertThat(result.getName()).isEqualTo("Dune");
        assertThat(result.getPrice()).isEqualByComparingTo("10.00");
    }

    @Test
    void getBookByName_notFound_throws() {
        when(bookRepository.findByName("Unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getBookByName("Unknown"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void addBook_newName_saves() {
        when(bookRepository.existsByName("Dune")).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookDTO result = bookService.addBook(bookDto("Dune", "10.00"));

        assertThat(result.getName()).isEqualTo("Dune");
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void addBook_existingName_throwsAndDoesNotSave() {
        when(bookRepository.existsByName("Dune")).thenReturn(true);

        assertThatThrownBy(() -> bookService.addBook(bookDto("Dune", "10.00")))
                .isInstanceOf(AlreadyExistException.class);

        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void updateBookByName_sameName_updatesFieldsAndKeepsId() {
        Book existing = book("Dune", "10.00");
        when(bookRepository.findByName("Dune")).thenReturn(Optional.of(existing));
        when(bookRepository.save(existing)).thenReturn(existing);

        BookDTO result = bookService.updateBookByName("Dune", bookDto("Dune", "20.00"));

        assertThat(result.getPrice()).isEqualByComparingTo("20.00");
        assertThat(existing.getId()).isEqualTo(1L);
        // the name did not change, so there is no need to check that the name is free
        verify(bookRepository, never()).existsByName(any());
    }

    @Test
    void updateBookByName_renameToFreeName_updatesName() {
        Book existing = book("Dune", "10.00");
        when(bookRepository.findByName("Dune")).thenReturn(Optional.of(existing));
        when(bookRepository.existsByName("Emma")).thenReturn(false);
        when(bookRepository.save(existing)).thenReturn(existing);

        BookDTO result = bookService.updateBookByName("Dune", bookDto("Emma", "10.00"));

        assertThat(result.getName()).isEqualTo("Emma");
    }

    @Test
    void updateBookByName_renameToTakenName_throwsAndLeavesEntityUntouched() {
        Book existing = book("Dune", "10.00");
        when(bookRepository.findByName("Dune")).thenReturn(Optional.of(existing));
        when(bookRepository.existsByName("Emma")).thenReturn(true);

        assertThatThrownBy(() -> bookService.updateBookByName("Dune", bookDto("Emma", "10.00")))
                .isInstanceOf(AlreadyExistException.class);

        // the check happens BEFORE the mapping, so the managed entity is not modified
        assertThat(existing.getName()).isEqualTo("Dune");
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void updateBookByName_notFound_throws() {
        when(bookRepository.findByName("Unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.updateBookByName("Unknown", bookDto("Unknown", "10.00")))
                .isInstanceOf(NotFoundException.class);

        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void deleteBookByName_notInOrders_deletes() {
        when(bookRepository.existsByName("Dune")).thenReturn(true);
        when(orderRepository.existsByBookItemsBookName("Dune")).thenReturn(false);

        bookService.deleteBookByName("Dune");

        verify(bookRepository).deleteByName("Dune");
    }

    @Test
    void deleteBookByName_notFound_throwsWithoutChecksOrDelete() {
        when(bookRepository.existsByName("Unknown")).thenReturn(false);

        assertThatThrownBy(() -> bookService.deleteBookByName("Unknown"))
                .isInstanceOf(NotFoundException.class);

        verify(orderRepository, never()).existsByBookItemsBookName(any());
        verify(bookRepository, never()).deleteByName(any());
    }

    @Test
    void deleteBookByName_inOrders_throwsAndDoesNotDelete() {
        when(bookRepository.existsByName("Dune")).thenReturn(true);
        when(orderRepository.existsByBookItemsBookName("Dune")).thenReturn(true);

        assertThatThrownBy(() -> bookService.deleteBookByName("Dune"))
                .isInstanceOf(BookInUseException.class);

        verify(bookRepository, never()).deleteByName(any());
    }

    @Test
    void searchBooks_mapsPage() {
        Pageable pageable = PageRequest.of(0, 5);
        when(bookRepository.search("du", pageable))
                .thenReturn(new PageImpl<>(List.of(book("Dune", "10.00")), pageable, 1));

        Page<BookDTO> result = bookService.searchBooks("du", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).extracting(BookDTO::getName).containsExactly("Dune");
    }
}
