package com.zelia.agenda.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(@NotBlank @Email @Size(max = 255) String email,
                              @NotBlank @Size(max = 72) String password) {
}
