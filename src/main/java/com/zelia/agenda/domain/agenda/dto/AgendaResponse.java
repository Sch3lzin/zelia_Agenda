package com.zelia.agenda.domain.agenda.dto;

import com.zelia.agenda.domain.agenda.Agenda;
import com.zelia.agenda.domain.agenda.ClassTime;
import com.zelia.agenda.domain.schoolClass.dto.SchoolClassResponse;
import com.zelia.agenda.domain.user.dto.UserResponse;

import java.time.LocalDate;

public record AgendaResponse(Long id, UserResponse user, SchoolClassResponse schoolClass, String observation, ClassTime classTime, LocalDate date) {

    public static AgendaResponse from(Agenda agenda) {
        return new AgendaResponse(
                agenda.getId(),
                UserResponse.from(agenda.getUser()),
                SchoolClassResponse.from(agenda.getSchoolClass()),
                agenda.getObservation(),
                agenda.getClassTime(),
                agenda.getDate()
        );
    }
}