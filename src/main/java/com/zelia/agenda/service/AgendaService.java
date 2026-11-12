package com.zelia.agenda.service;

import com.zelia.agenda.domain.agenda.Agenda;
import com.zelia.agenda.domain.agenda.AgendaRepository;
import com.zelia.agenda.domain.agenda.dto.CreateAgendaDTO;
import com.zelia.agenda.domain.schoolClass.SchoolClass;
import com.zelia.agenda.domain.schoolClass.SchoolClassRepository;
import com.zelia.agenda.domain.user.User;
import com.zelia.agenda.domain.user.UserRepository;
import com.zelia.agenda.exceptions.AgendaAlreadyExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AgendaService {

    private final AgendaRepository agendaRepository;
    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;

    public AgendaService(AgendaRepository agendaRepository, UserRepository userRepository, SchoolClassRepository schoolClassRepository) {
        this.agendaRepository = agendaRepository;
        this.userRepository = userRepository;
        this.schoolClassRepository = schoolClassRepository;
    }

    public Agenda createAgenda(Long id, CreateAgendaDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario não encontrado: " + id));

        SchoolClass schoolClass = schoolClassRepository.findById(dto.schoolClassId())
                .orElseThrow(() -> new EntityNotFoundException("Turma não encontrada: " + id));

        if (agendaRepository.existsByPeriodAndClassTimeAndDate(dto.period(), dto.classTime(), dto.date())) {
            throw new AgendaAlreadyExistsException(
                    "Já existe um agendamento para o período " + dto.period() +
                    ", horário de aula " + dto.classTime() + " na data " + dto.date());
        }

        Agenda agenda = new Agenda(
                user,
                schoolClass,
                dto.observaion(),
                dto.period(),
                dto.classTime(),
                dto.date()
        );

        return agendaRepository.save(agenda);
    }
}
