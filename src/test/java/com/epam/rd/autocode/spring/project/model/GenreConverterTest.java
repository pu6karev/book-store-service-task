package com.epam.rd.autocode.spring.project.model;

import com.epam.rd.autocode.spring.project.model.enums.Genre;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GenreConverterTest {

    private final GenreConverter converter = new GenreConverter();

    // ---------- convertToDatabaseColumn ----------

    @Test
    void convertToDatabaseColumn_whenGenre_shouldReturnDisplayText() {
        assertEquals("Science Fiction", converter.convertToDatabaseColumn(Genre.SCIENCE_FICTION));
    }

    @Test
    void convertToDatabaseColumn_whenNull_shouldReturnNull() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    // ---------- convertToEntityAttribute ----------

    @Test
    void convertToEntityAttribute_whenKnownText_shouldReturnGenre() {
        assertEquals(Genre.HISTORICAL_FICTION, converter.convertToEntityAttribute("Historical Fiction"));
    }

    @Test
    void convertToEntityAttribute_whenUnknownText_shouldReturnOther() {
        assertEquals(Genre.OTHER, converter.convertToEntityAttribute("Poetry"));
    }

    @Test
    void convertToEntityAttribute_whenNull_shouldReturnNull() {
        assertNull(converter.convertToEntityAttribute(null));
    }

    // ---------- round trip ----------

    @ParameterizedTest
    @EnumSource(Genre.class)
    void roundTrip_whenAnyGenre_shouldReturnTheSameGenre(Genre genre) {
        String stored = converter.convertToDatabaseColumn(genre);

        assertEquals(genre, converter.convertToEntityAttribute(stored));
    }
}