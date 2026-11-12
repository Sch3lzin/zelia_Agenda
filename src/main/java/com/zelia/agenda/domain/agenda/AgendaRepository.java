package com.zelia.agenda.domain.agenda;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface AgendaRepository extends JpaRepository<Agenda, Long> {

    boolean existsByPeriodAndClassTimeAndDate(Period period, ClassTime classTime, LocalDate date);
}
