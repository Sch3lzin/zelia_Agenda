package com.zelia.agenda.domain.schoolClass;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

    Optional<SchoolClass> findByName(String name);
}
