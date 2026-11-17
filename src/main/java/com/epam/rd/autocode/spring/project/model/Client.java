package com.epam.rd.autocode.spring.project.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "clients")
@NoArgsConstructor
public class Client extends User{
    public Client(Long id, String name, String email, String password, BigDecimal balance) {
        super(id, name, email, password);
        this.balance = balance;
    }

    private BigDecimal balance;

    // the default lets INSERTs without this column (V2__initial_data.sql) work
    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean blocked;
}
