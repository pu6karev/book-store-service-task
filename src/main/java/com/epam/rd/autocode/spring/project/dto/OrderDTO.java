package com.epam.rd.autocode.spring.project.dto;

import com.epam.rd.autocode.spring.project.validation.ValidEmail;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    // id, orderDate and price are set by the server: they are returned in responses and ignored in requests
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotBlank(message = "Client email must not be blank")
    @ValidEmail
    private String clientEmail;

    @ValidEmail
    private String employeeEmail;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime orderDate;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal price;

    @NotEmpty(message = "Order must contain at least one book")
    @Valid
    private List<BookItemDTO> bookItems = new ArrayList<>();
}
