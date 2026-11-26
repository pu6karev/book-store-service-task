package com.epam.rd.autocode.spring.project.dto;

import com.epam.rd.autocode.spring.project.model.enums.AgeGroup;
import com.epam.rd.autocode.spring.project.model.enums.Genre;
import com.epam.rd.autocode.spring.project.model.enums.Language;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookDTO {
    // The name is used as an identifier in URLs (/api/books/{name}),
    // so characters that have a special meaning in a URL are forbidden.
    // "*" (not "+") so that an empty value gives only the @NotBlank error.
    @NotBlank(message = "{validation.book.name.notblank}")
    @Pattern(regexp = "[^/\\\\?#%;]*", message = "{validation.book.name.chars}")
    @Size(max = 255, message = "{validation.size.max}")
    private String name;

    @NotNull(message = "{validation.book.genre.notnull}")
    private Genre genre;

    @NotNull(message = "{validation.book.agegroup.notnull}")
    private AgeGroup ageGroup;

    @NotNull(message = "{validation.book.price.notnull}")
    @Positive(message = "{validation.book.price.positive}")
    private BigDecimal price;

    // without @DateTimeFormat the default (locale-dependent) format would be used and "2018-05-15" from <input type="date"> would not bind
    @PastOrPresent(message = "{validation.book.date.past}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate publicationDate;

    @NotBlank(message = "{validation.book.author.notblank}")
    @Size(max = 255, message = "{validation.size.max}")
    private String author;

    @NotNull(message = "{validation.book.pages.notnull}")
    @Min(value = 1, message = "{validation.book.pages.min}")
    private Integer pages;

    @Size(max = 255, message = "{validation.size.max}")
    private String characteristics;

    @Size(max = 255, message = "{validation.size.max}")
    private String description;

    @NotNull(message = "{validation.book.language.notnull}")
    private Language language;
}