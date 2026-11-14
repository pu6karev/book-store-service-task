package com.epam.rd.autocode.spring.project.dto;

import com.epam.rd.autocode.spring.project.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


// The client should not be able to set their own balance, so ClientDTO is not suitable for this purpose.
@Getter
@Setter
@NoArgsConstructor
public class ClientRegistrationDTO {
    @NotBlank(message = "{validation.name.notblank}")
    private String name;

    @NotBlank(message = "{validation.email.notblank}")
    @Email(message = "{validation.email.invalid}")
    private String email;

    @NotBlank(message = "{validation.password.notblank}")
    @StrongPassword
    private String password;

    @NotBlank(message = "{validation.password.confirm.notblank}")
    private String confirmPassword;
}
