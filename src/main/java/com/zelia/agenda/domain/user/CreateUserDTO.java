package com.zelia.agenda.domain.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserDTO(@NotBlank String name,
                            @NotBlank String email,
                            @NotBlank String password,
                            @NotNull UserType userType) {
}
