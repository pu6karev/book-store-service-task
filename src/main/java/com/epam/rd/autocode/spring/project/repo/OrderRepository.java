package com.epam.rd.autocode.spring.project.repo;

import com.epam.rd.autocode.spring.project.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findAllByClientEmail(String email);

    List<Order> findAllByEmployeeEmail(String email);

    // path: Order.bookItems -> BookItem.book -> Book.name
    boolean existsByBookItemsBookName(String bookName);

    // orders without an employee are not confirmed yet; the oldest first
    List<Order> findAllByEmployeeIsNullOrderByOrderDateAsc();

    boolean existsByClientEmail(String email);
}
