package com.epam.rd.autocode.spring.project.dto;

import com.epam.rd.autocode.spring.project.model.enums.AgeGroup;
import com.epam.rd.autocode.spring.project.model.enums.Language;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookDTO{
    @NotBlank(message = "Book name must not be blank")
    private String name;

    @NotBlank(message = "Book genre must not be blank")
    private String genre;

    @NotNull(message = "Age group must not be null")
    private AgeGroup ageGroup;

    @NotNull(message = "Price must not be null")
    @Positive(message = "Price must be greater than zero")
    private BigDecimal price;

    @PastOrPresent(message = "Publication date cannot be in the future")
    private LocalDate publicationDate;

    @NotBlank(message = "Book author must not be blank")
    private String author;

    @NotNull(message = "Number of pages must not be null")
    @Min(value = 1, message = "Number of pages must be at least 1")
    private Integer pages;
    private String characteristics;
    private String description;

    @NotNull(message = "Language must not be null")
    private Language language;
}
