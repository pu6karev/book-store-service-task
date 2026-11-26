package com.epam.rd.autocode.spring.project.repo;

import com.epam.rd.autocode.spring.project.model.Book;
import com.epam.rd.autocode.spring.project.model.enums.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByName(String name);
    void deleteByName(String name);
    boolean existsByName(String name);

    // genre == null means "any genre"; the parentheses around OR are required, otherwise the genre filter
    // would apply only to the author part of the condition
    @Query("""
       SELECT b FROM Book b
       WHERE (:genre IS NULL OR b.genre = :genre)
         AND (LOWER(b.name)   LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(b.author) LIKE LOWER(CONCAT('%', :q, '%')))
       """)
    Page<Book> search(@Param("q") String q, @Param("genre") Genre genre, Pageable pageable);
}
