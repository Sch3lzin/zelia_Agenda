package com.zelia.agenda.domain.agenda.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record GetListDayDTO(@NotNull LocalDate date) {
}
