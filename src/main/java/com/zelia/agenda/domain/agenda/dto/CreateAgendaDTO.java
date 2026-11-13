package com.zelia.agenda.domain.agenda.dto;

import com.zelia.agenda.domain.agenda.ClassTime;
import com.zelia.agenda.domain.agenda.Period;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateAgendaDTO(@NotNull Long schoolClassId, @Size(max = 255) String observation, @NotNull Period period, @NotNull ClassTime classTime, @NotNull LocalDate date) {
}
