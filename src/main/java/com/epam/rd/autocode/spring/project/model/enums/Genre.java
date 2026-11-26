package com.epam.rd.autocode.spring.project.model.enums;

public enum Genre {
    ADVENTURE("Adventure"),
    FANTASY("Fantasy"),
    MYSTERY("Mystery"),
    ROMANCE("Romance"),
    SCIENCE_FICTION("Science Fiction"),
    THRILLER("Thriller"),
    HISTORICAL_FICTION("Historical Fiction"),
    OTHER("Other");

    // The text shown in the UI. It is also the value stored in the books.genre column
    // (see GenreConverter), so the column and the existing data stay as they were.
    private final String displayName;

    Genre(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // A text that is not in the list (for example a free-text genre written before the enum existed)
    // becomes OTHER instead of breaking the page.
    public static Genre fromDisplayName(String value) {
        for (Genre genre : values()) {
            if (genre.displayName.equals(value)) {
                return genre;
            }
        }
        return OTHER;
    }
}
