package com.zelia.agenda.domain.schoolClass.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateClassDTO (@NotBlank String year,@NotBlank String number){
}
