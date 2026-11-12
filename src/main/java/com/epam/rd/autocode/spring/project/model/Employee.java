package com.epam.rd.autocode.spring.project.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "employees")
@NoArgsConstructor
public class Employee extends User{
    public Employee(Long id, String name, String email, String password, String phone, LocalDate birthDate) {
        super(id, name, email, password);
        this.phone = phone;
        this.birthDate = birthDate;
    }

    private String phone;
    private LocalDate birthDate;
}
