package com.zelia.agenda.service;

import com.zelia.agenda.domain.agenda.Agenda;
import com.zelia.agenda.domain.agenda.AgendaRepository;
import com.zelia.agenda.domain.agenda.ClassTime;
import com.zelia.agenda.domain.agenda.Period;
import com.zelia.agenda.domain.agenda.dto.AgendaResponseList;
import com.zelia.agenda.domain.agenda.dto.CreateAgendaDTO;
import com.zelia.agenda.domain.agenda.dto.GetListDayDTO;
import com.zelia.agenda.domain.schoolClass.SchoolClass;
import com.zelia.agenda.domain.schoolClass.SchoolClassRepository;
import com.zelia.agenda.domain.user.User;
import com.zelia.agenda.domain.user.UserRepository;
import com.zelia.agenda.domain.user.UserType;
import com.zelia.agenda.exceptions.AgendaAlreadyExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgendaServiceTest {

    private static final LocalDate DATA = LocalDate.of(2026, 11, 18);

    @Mock
    private AgendaRepository agendaRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SchoolClassRepository schoolClassRepository;

    private AgendaService service;

    @BeforeEach
    void setUp() {
        service = new AgendaService(agendaRepository, userRepository, schoolClassRepository);
    }

    private static User user(long id) {
        User user = new User("Ana", "ana@exemplo.com", "hash", UserType.PROFESSOR, true);
        user.setId(id);
        return user;
    }

    private static SchoolClass schoolClass(long id) {
        SchoolClass schoolClass = new SchoolClass("8\u00b0 I");
        schoolClass.setId(id);
        return schoolClass;
    }

    private static Agenda agenda(long id, User dono) {
        Agenda agenda = new Agenda(dono, schoolClass(1L), "obs", Period.MATUTINO, ClassTime.AULA_1, DATA);
        agenda.setId(id);
        return agenda;
    }

    private static CreateAgendaDTO newDto() {
        return new CreateAgendaDTO(3L, "obs", Period.VESPERTINO, ClassTime.AULA_5, DATA);
    }

    private static Authentication authentication(String papel) {
        Authentication auth = mock(Authentication.class);
        doReturn(List.of(new SimpleGrantedAuthority(papel))).when(auth).getAuthorities();
        return auth;
    }

    // ---------- consultas ----------

    @Test
    void getUnknownAgendaThrowsNotFound() {
        when(agendaRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAgenda(9L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void dayListSplitsMorningAndAfternoon() {
        User ana = user(1L);
        Agenda vespertino = new Agenda(ana, schoolClass(1L), "", Period.VESPERTINO, ClassTime.AULA_5, DATA);
        vespertino.setId(2L);
        when(agendaRepository.findByDate(DATA)).thenReturn(List.of(agenda(1L, ana), vespertino));

        AgendaResponseList lista = service.getListAgendaDay(new GetListDayDTO(DATA));

        assertThat(lista.matutino()).hasSize(1);
        assertThat(lista.matutino().get(0).period()).isEqualTo(Period.MATUTINO);
        assertThat(lista.vespertino()).hasSize(1);
        assertThat(lista.vespertino().get(0).classTime()).isEqualTo(ClassTime.AULA_5);
    }

    // ---------- criação ----------

    @Test
    void createAgendaWithUnknownUserFails() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createAgenda(9L, newDto()))
                .isInstanceOf(EntityNotFoundException.class);
        verify(agendaRepository, never()).save(any(Agenda.class));
    }

    @Test
    void createAgendaWithUnknownClassFails() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(schoolClassRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createAgenda(1L, newDto()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void createAgendaInOccupiedSlotFails() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(schoolClassRepository.findById(3L)).thenReturn(Optional.of(schoolClass(3L)));
        when(agendaRepository.existsByPeriodAndClassTimeAndDate(Period.VESPERTINO, ClassTime.AULA_5, DATA))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createAgenda(1L, newDto()))
                .isInstanceOf(AgendaAlreadyExistsException.class)
                .hasMessageContaining("VESPERTINO");
        verify(agendaRepository, never()).save(any(Agenda.class));
    }

    @Test
    void createAgendaLinksLoggedUserAndClass() {
        User ana = user(1L);
        SchoolClass sala = schoolClass(3L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(ana));
        when(schoolClassRepository.findById(3L)).thenReturn(Optional.of(sala));
        when(agendaRepository.save(any(Agenda.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Agenda> captor = ArgumentCaptor.forClass(Agenda.class);
        service.createAgenda(1L, newDto());
        verify(agendaRepository).save(captor.capture());

        Agenda salvo = captor.getValue();
        assertThat(salvo.getUser()).isSameAs(ana);
        assertThat(salvo.getSchoolClass()).isSameAs(sala);
        assertThat(salvo.getPeriod()).isEqualTo(Period.VESPERTINO);
        assertThat(salvo.getClassTime()).isEqualTo(ClassTime.AULA_5);
        assertThat(salvo.getDate()).isEqualTo(DATA);
        assertThat(salvo.getObservation()).isEqualTo("obs");
    }

    @Test
    void databaseRaceBecomesOccupiedSlotError() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(schoolClassRepository.findById(3L)).thenReturn(Optional.of(schoolClass(3L)));
        when(agendaRepository.save(any(Agenda.class)))
                .thenThrow(new DataIntegrityViolationException("unique"));

        assertThatThrownBy(() -> service.createAgenda(1L, newDto()))
                .isInstanceOf(AgendaAlreadyExistsException.class);
    }

    // ---------- atualização ----------

    @Test
    void updateUnknownAgendaThrowsNotFound() {
        when(agendaRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateAgenda(9L, 1L, newDto(), mock(Authentication.class)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void nonOwnerProfessorCannotUpdate() {
        when(agendaRepository.findById(10L)).thenReturn(Optional.of(agenda(10L, user(2L))));

        assertThatThrownBy(() -> service.updateAgenda(10L, 1L, newDto(), authentication("ROLE_PROFESSOR")))
                .isInstanceOf(AccessDeniedException.class);
        verify(agendaRepository, never()).save(any(Agenda.class));
    }

    @Test
    void ownerUpdatesOwnAgenda() {
        Agenda existente = agenda(10L, user(1L));
        when(agendaRepository.findById(10L)).thenReturn(Optional.of(existente));
        when(schoolClassRepository.findById(3L)).thenReturn(Optional.of(schoolClass(3L)));
        when(agendaRepository.save(any(Agenda.class))).thenAnswer(inv -> inv.getArgument(0));

        Agenda atualizado = service.updateAgenda(10L, 1L, newDto(), authentication("ROLE_PROFESSOR"));

        assertThat(atualizado.getPeriod()).isEqualTo(Period.VESPERTINO);
        assertThat(atualizado.getClassTime()).isEqualTo(ClassTime.AULA_5);
        assertThat(atualizado.getSchoolClass().getId()).isEqualTo(3L);
    }

    @Test
    void adminUpdatesAnotherProfessorsAgenda() {
        Agenda existente = agenda(10L, user(2L));
        when(agendaRepository.findById(10L)).thenReturn(Optional.of(existente));
        when(schoolClassRepository.findById(3L)).thenReturn(Optional.of(schoolClass(3L)));
        when(agendaRepository.save(any(Agenda.class))).thenAnswer(inv -> inv.getArgument(0));

        Agenda atualizado = service.updateAgenda(10L, 1L, newDto(), authentication("ROLE_ADMIN"));

        assertThat(atualizado.getClassTime()).isEqualTo(ClassTime.AULA_5);
    }

    @Test
    void updateToOccupiedSlotFails() {
        when(agendaRepository.findById(10L)).thenReturn(Optional.of(agenda(10L, user(1L))));
        when(schoolClassRepository.findById(3L)).thenReturn(Optional.of(schoolClass(3L)));
        when(agendaRepository.existsByPeriodAndClassTimeAndDateAndIdNot(
                Period.VESPERTINO, ClassTime.AULA_5, DATA, 10L)).thenReturn(true);

        assertThatThrownBy(() -> service.updateAgenda(10L, 1L, newDto(), authentication("ROLE_PROFESSOR")))
                .isInstanceOf(AgendaAlreadyExistsException.class);
        verify(agendaRepository, never()).save(any(Agenda.class));
    }

    // ---------- exclusão ----------

    @Test
    void deleteUnknownAgendaThrowsNotFound() {
        when(agendaRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteAgenda(9L, 1L, mock(Authentication.class)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void nonOwnerProfessorCannotDelete() {
        when(agendaRepository.findById(10L)).thenReturn(Optional.of(agenda(10L, user(2L))));

        assertThatThrownBy(() -> service.deleteAgenda(10L, 1L, authentication("ROLE_PROFESSOR")))
                .isInstanceOf(AccessDeniedException.class);
        verify(agendaRepository, never()).delete(any(Agenda.class));
    }

    @Test
    void ownerDeletesOwnAgenda() {
        Agenda existente = agenda(10L, user(1L));
        when(agendaRepository.findById(10L)).thenReturn(Optional.of(existente));

        service.deleteAgenda(10L, 1L, authentication("ROLE_PROFESSOR"));

        verify(agendaRepository).delete(existente);
    }

    @Test
    void adminDeletesAnotherProfessorsAgenda() {
        Agenda existente = agenda(10L, user(2L));
        when(agendaRepository.findById(10L)).thenReturn(Optional.of(existente));

        service.deleteAgenda(10L, 1L, authentication("ROLE_ADMIN"));

        verify(agendaRepository).delete(existente);
    }
}
