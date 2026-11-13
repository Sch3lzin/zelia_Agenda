package com.zelia.agenda.service;

import com.zelia.agenda.domain.agenda.Agenda;
import com.zelia.agenda.domain.agenda.AgendaRepository;
import com.zelia.agenda.domain.agenda.Period;
import com.zelia.agenda.domain.agenda.dto.AgendaResponse;
import com.zelia.agenda.domain.agenda.dto.AgendaResponseList;
import com.zelia.agenda.domain.agenda.dto.CreateAgendaDTO;
import com.zelia.agenda.domain.agenda.dto.GetListDayDTO;
import com.zelia.agenda.domain.schoolClass.SchoolClass;
import com.zelia.agenda.domain.schoolClass.SchoolClassRepository;
import com.zelia.agenda.domain.user.User;
import com.zelia.agenda.domain.user.UserRepository;
import com.zelia.agenda.exceptions.AgendaAlreadyExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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

    public Agenda getAgenda(Long id) {
        Agenda agenda = agendaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario não encontrado: " + id));

        return agenda;
    }

    public AgendaResponseList getListAgendaDay(GetListDayDTO dto) {
        List<Agenda> agendas = agendaRepository.findByDate(dto.date());

        List<AgendaResponse> agendaMatutino = agendas.stream()
                .filter(a -> a.getPeriod() == Period.MATUTINO)
                .map(AgendaResponse::from)
                .collect(Collectors.toList());

        List<AgendaResponse> agendaVespertina = agendas.stream()
                .filter(a -> a.getPeriod() != Period.MATUTINO)
                .map(AgendaResponse::from)
                .collect(Collectors.toList());

        return new AgendaResponseList(agendaMatutino, agendaVespertina);
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

    public Agenda updateAgenda(Long id, Long userId, CreateAgendaDTO dto, Authentication authentication) {
        Agenda agenda = agendaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado: " + id));

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        boolean isOwner = agenda.getUser().getId().equals(userId);

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Você não tem permissão para atualizar esta agenda.");
        }

        SchoolClass schoolClass = schoolClassRepository.findById(dto.schoolClassId())
                .orElseThrow(() -> new EntityNotFoundException("Turma não encontrada: " + dto.schoolClassId()));

        boolean conflito = agendaRepository
                .existsByPeriodAndClassTimeAndDateAndIdNot(dto.period(), dto.classTime(), dto.date(), id);

        if (conflito) {
            throw new AgendaAlreadyExistsException(
                    "Já existe um agendamento para o período " + dto.period() +
                            ", horário de aula " + dto.classTime() + " na data " + dto.date());
        }

        agenda.setSchoolClass(schoolClass);
        agenda.setObservation(dto.observaion());
        agenda.setPeriod(dto.period());
        agenda.setClassTime(dto.classTime());
        agenda.setDate(dto.date());

        return agendaRepository.save(agenda);
    }

    public void deleteAgenda(Long id, Long userId, Authentication authentication) {
        Agenda agenda = agendaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado: " + id));

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        boolean isOwner = agenda.getUser().getId().equals(userId);

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Você não tem permissão para deletar esta agenda.");
        }

        agendaRepository.delete(agenda);
    }
}
