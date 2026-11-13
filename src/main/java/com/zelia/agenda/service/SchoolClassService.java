package com.zelia.agenda.service;

import com.zelia.agenda.domain.agenda.AgendaRepository;
import com.zelia.agenda.domain.schoolClass.SchoolClassRepository;
import com.zelia.agenda.domain.schoolClass.SchoolClass;
import com.zelia.agenda.domain.schoolClass.dto.CreateClassDTO;
import com.zelia.agenda.exceptions.BusinessRuleException;
import com.zelia.agenda.exceptions.ClassAlreadyExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SchoolClassService {

    private final SchoolClassRepository schoolClassRepository;
    private final AgendaRepository agendaRepository;

    public SchoolClassService(SchoolClassRepository schoolClassRepository, AgendaRepository agendaRepository) {
        this.schoolClassRepository = schoolClassRepository;
        this.agendaRepository = agendaRepository;
    }

    public SchoolClass createClass(CreateClassDTO dto) {
        String name = dto.year() + "° " + dto.number();

        if (schoolClassRepository.findByName(name).isPresent()) {
            throw new ClassAlreadyExistsException(name);
        }

        try {
            return schoolClassRepository.save(new SchoolClass(name));
        } catch (DataIntegrityViolationException e) {
            // Turma criada por outra requisição ao mesmo tempo.
            throw new ClassAlreadyExistsException(name);
        }
    }

    public void deleteClass(Long id) {
        SchoolClass schoolClass = schoolClassRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Turma não encontrada: " + id));

        if (agendaRepository.existsBySchoolClassId(id)) {
            throw new BusinessRuleException("Não é possível excluir a turma: existem agendamentos vinculados a ela.");
        }

        schoolClassRepository.delete(schoolClass);
    }

    public List<SchoolClass> getAllClasses() {
        return schoolClassRepository.findAll();
    }
}
