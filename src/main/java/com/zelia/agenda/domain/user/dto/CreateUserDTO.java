package com.zelia.agenda.domain.user.dto;

import com.zelia.agenda.domain.user.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserDTO(@NotBlank @Size(max = 255) String name,
                            @NotBlank @Email @Size(max = 255) String email,
                            @NotBlank @Size(min = 8, max = 72) String password,
                            @NotNull UserType userType) {
}
