package com.zelia.agenda.domain.agenda;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AgendaRepository extends JpaRepository<Agenda, Long> {

    boolean existsByPeriodAndClassTimeAndDate(Period period, ClassTime classTime, LocalDate date);

    List<Agenda> findByDate(@NotNull LocalDate date);

    boolean existsByPeriodAndClassTimeAndDateAndIdNot(@NotNull Period period, @NotNull ClassTime classTime, @NotNull LocalDate date, Long id);

    boolean existsBySchoolClassId(Long schoolClassId);
}
