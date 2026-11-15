package com.epam.rd.autocode.spring.project.dto;

import com.epam.rd.autocode.spring.project.validation.StrongPassword;
import com.epam.rd.autocode.spring.project.validation.ValidEmail;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClientDTO{
    @NotBlank(message = "Client name must not be blank")
    private String name;

    @NotBlank(message = "Email must not be blank")
    @ValidEmail
    private String email;

    @NotBlank(message = "Password must not be blank")
    @StrongPassword
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @NotNull(message = "Balance must not be null")
    @PositiveOrZero(message = "Balance cannot be negative")
    private BigDecimal balance;
}
