package com.epam.rd.autocode.spring.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class ProfileDTO {
    @NotBlank(message = "{validation.name.notblank}")
    private String name;

    // it's necessary only for the employee; a controller verifies that it is indeed performed by them.
    @Pattern(regexp = "^(\\+?[0-9\\s()-]{7,20})?$", message = "{validation.phone.invalid}")
    private String phone;

    @Past(message = "{validation.birthdate.past}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthDate;
}
