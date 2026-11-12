package com.epam.rd.autocode.spring.project.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO{
    @NotBlank(message = "Client email must not be blank")
    @Email(message = "Invalid client email format")
    private String clientEmail;

    @Email(message = "Invalid employee email format")
    private String employeeEmail;

    @PastOrPresent(message = "Order date cannot be in the future")
    private LocalDateTime orderDate;

    @NotNull(message = "Price must not be null")
    @Positive(message = "Price must be greater than zero")
    private BigDecimal price;

    @NotEmpty(message = "Order must contain at least one book")
    @Valid
    private List<BookItemDTO> bookItems = new ArrayList<>();
}
