package com.zelia.agenda.service;

import com.zelia.agenda.domain.agenda.AgendaRepository;
import com.zelia.agenda.domain.schoolClass.SchoolClass;
import com.zelia.agenda.domain.schoolClass.SchoolClassRepository;
import com.zelia.agenda.domain.schoolClass.dto.CreateClassDTO;
import com.zelia.agenda.exceptions.BusinessRuleException;
import com.zelia.agenda.exceptions.ClassAlreadyExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolClassServiceTest {

    // Class name format: "8° I"
    private static final String NOME_8_I = "8\u00b0 I";

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @Mock
    private AgendaRepository agendaRepository;

    private SchoolClassService service;

    @BeforeEach
    void setUp() {
        service = new SchoolClassService(schoolClassRepository, agendaRepository);
    }

    @Test
    void createClassBuildsNameFromYearAndNumber() {
        when(schoolClassRepository.findByName(NOME_8_I)).thenReturn(Optional.empty());
        when(schoolClassRepository.save(any(SchoolClass.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<SchoolClass> captor = ArgumentCaptor.forClass(SchoolClass.class);
        SchoolClass criada = service.createClass(new CreateClassDTO("8", "I"));

        verify(schoolClassRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo(NOME_8_I);
        assertThat(criada.getName()).isEqualTo(NOME_8_I);
    }

    @Test
    void createDuplicateClassFails() {
        when(schoolClassRepository.findByName(NOME_8_I))
                .thenReturn(Optional.of(new SchoolClass(NOME_8_I)));

        assertThatThrownBy(() -> service.createClass(new CreateClassDTO("8", "I")))
                .isInstanceOf(ClassAlreadyExistsException.class);
        verify(schoolClassRepository, never()).save(any(SchoolClass.class));
    }

    @Test
    void databaseRaceBecomesDuplicateClassError() {
        when(schoolClassRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(schoolClassRepository.save(any(SchoolClass.class)))
                .thenThrow(new DataIntegrityViolationException("unique"));

        assertThatThrownBy(() -> service.createClass(new CreateClassDTO("8", "I")))
                .isInstanceOf(ClassAlreadyExistsException.class);
    }

    @Test
    void deleteUnknownClassThrowsNotFound() {
        when(schoolClassRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteClass(9L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deleteClassWithAgendasFails() {
        SchoolClass turma = new SchoolClass(NOME_8_I);
        when(schoolClassRepository.findById(1L)).thenReturn(Optional.of(turma));
        when(agendaRepository.existsBySchoolClassId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteClass(1L))
                .isInstanceOf(BusinessRuleException.class);
        verify(schoolClassRepository, never()).delete(any(SchoolClass.class));
    }

    @Test
    void deleteClassWithoutAgendasDeletesIt() {
        SchoolClass turma = new SchoolClass(NOME_8_I);
        when(schoolClassRepository.findById(1L)).thenReturn(Optional.of(turma));
        when(agendaRepository.existsBySchoolClassId(1L)).thenReturn(false);

        service.deleteClass(1L);

        verify(schoolClassRepository).delete(turma);
    }

    @Test
    void listClassesReturnsAll() {
        when(schoolClassRepository.findAll()).thenReturn(List.of(new SchoolClass(NOME_8_I)));

        assertThat(service.getAllClasses()).hasSize(1);
    }
}
