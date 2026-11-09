package com.zelia.agenda.service;

import com.zelia.agenda.domain.schoolClass.SchoolClassRepository;
import com.zelia.agenda.domain.schoolClass.SchoolClass;
import com.zelia.agenda.domain.schoolClass.dto.CreateClassDTO;
import com.zelia.agenda.exceptions.ClassAlreadyExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SchoolClassService {

    private final SchoolClassRepository schoolClassRepository;

    public SchoolClassService(SchoolClassRepository schoolClassRepository) {
        this.schoolClassRepository = schoolClassRepository;
    }

    public SchoolClass createClass(CreateClassDTO dto) {
        String name = dto.year() + "° " + dto.number();

        if (schoolClassRepository.findByName(name).isPresent()) {
            throw new ClassAlreadyExistsException(name);
        }

        SchoolClass schoolClass = new SchoolClass(name);
        return schoolClassRepository.save(schoolClass);
    }

    public void deleteClass(Long id) {
        SchoolClass schoolClass = schoolClassRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Turma não encontrada: " + id));

        schoolClassRepository.delete(schoolClass);
    }

    public List<SchoolClass> getAllClasses() {
        return schoolClassRepository.findAll();
    }
}
