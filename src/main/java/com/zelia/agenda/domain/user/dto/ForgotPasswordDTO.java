package com.zelia.agenda.domain.user.dto;

import jakarta.validation.Valid;

public record ForgotPasswordDTO(@Valid String email) {
}
