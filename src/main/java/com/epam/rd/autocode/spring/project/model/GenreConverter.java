package com.epam.rd.autocode.spring.project.model;

import com.epam.rd.autocode.spring.project.model.enums.Genre;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// Stores a Genre in the books.genre column as its display text ("Science Fiction") and reads it back.
// The column stays a plain VARCHAR, so the data script V2 and the Flyway migrations do not change.
@Converter
public class GenreConverter implements AttributeConverter<Genre, String> {

    @Override
    public String convertToDatabaseColumn(Genre genre) {
        return genre == null ? null : genre.getDisplayName();
    }

    @Override
    public Genre convertToEntityAttribute(String value) {
        return value == null ? null : Genre.fromDisplayName(value);
    }
}
