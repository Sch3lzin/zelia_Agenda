package com.zelia.agenda.controllers;

import com.zelia.agenda.domain.schoolClass.SchoolClass;
import com.zelia.agenda.domain.schoolClass.dto.CreateClassDTO;
import com.zelia.agenda.domain.schoolClass.dto.SchoolClassResponse;
import com.zelia.agenda.service.SchoolClassService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/class")
public class SchoolClassController {

    private final SchoolClassService schoolClassService;

    public SchoolClassController(SchoolClassService schoolClassService) {
        this.schoolClassService = schoolClassService;
    }

    @GetMapping
    public ResponseEntity<List<SchoolClassResponse>> getAllClasses() {
        List<SchoolClassResponse> classes = schoolClassService.getAllClasses()
                .stream()
                .map(SchoolClassResponse::from)
                .toList();

        return ResponseEntity.ok(classes);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<SchoolClassResponse> createClass(@RequestBody @Valid CreateClassDTO dto) {
        SchoolClass schoolClass = schoolClassService.createClass(dto);
        return ResponseEntity.ok(SchoolClassResponse.from(schoolClass));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClass(@PathVariable Long id) {
        schoolClassService.deleteClass(id);
        return ResponseEntity.noContent().build();
    }
}
