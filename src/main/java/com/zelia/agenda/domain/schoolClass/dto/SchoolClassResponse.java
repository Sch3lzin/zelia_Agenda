package com.zelia.agenda.domain.schoolClass.dto;

import com.zelia.agenda.domain.schoolClass.SchoolClass;

public record SchoolClassResponse(Long id, String name) {

    public static SchoolClassResponse from(SchoolClass schoolClass) {
        return new SchoolClassResponse(schoolClass.getId(), schoolClass.getName());
    }
}
