package com.zelia.agenda.domain.schoolClass.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClassDTO (@NotBlank @Size(max = 20) String year, @NotBlank @Size(max = 20) String number){
}
