package com.zelia.agenda.domain.user.dto;

import com.zelia.agenda.domain.user.UserType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserDTO(@NotBlank String name,
                            @NotBlank String email,
                            @NotBlank String password,
                            @NotNull UserType userType) {
}
