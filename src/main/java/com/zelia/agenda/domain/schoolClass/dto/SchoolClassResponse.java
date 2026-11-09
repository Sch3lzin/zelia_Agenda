package com.zelia.agenda.domain.schoolClass.dto;

import com.zelia.agenda.domain.schoolClass.SchoolClass;
import com.zelia.agenda.domain.user.User;
import com.zelia.agenda.domain.user.dto.UserResponse;
import jakarta.validation.constraints.NotBlank;

public record SchoolClassResponse(@NotBlank Long id, @NotBlank String name) {

    public static SchoolClassResponse from(SchoolClass schoolClass) {
        return new SchoolClassResponse(schoolClass.getId(), schoolClass.getName());
    }
}
