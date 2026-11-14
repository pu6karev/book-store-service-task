package com.epam.rd.autocode.spring.project.repo;

import com.epam.rd.autocode.spring.project.model.Book;
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

    @Query("""
       SELECT b FROM Book b
       WHERE LOWER(b.name)   LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(b.author) LIKE LOWER(CONCAT('%', :q, '%'))
       """)
    Page<Book> search(@Param("q") String q, Pageable pageable);
}
