package com.zelia.agenda.domain.agenda.dto;

import com.zelia.agenda.domain.agenda.Agenda;
import com.zelia.agenda.domain.agenda.ClassTime;
import com.zelia.agenda.domain.agenda.Period;
import com.zelia.agenda.domain.schoolClass.dto.SchoolClassResponse;
import com.zelia.agenda.domain.user.dto.UserResponse;
import com.zelia.agenda.domain.user.dto.UserResponseAgenda;

import java.time.LocalDate;

public record AgendaResponse(Long id, UserResponseAgenda user, SchoolClassResponse schoolClass, String observation, Period period, ClassTime classTime, LocalDate date) {

    public static AgendaResponse from(Agenda agenda) {
        return new AgendaResponse(
                agenda.getId(),
                UserResponseAgenda.from(agenda.getUser()),
                SchoolClassResponse.from(agenda.getSchoolClass()),
                agenda.getObservation(),
                agenda.getPeriod(),
                agenda.getClassTime(),
                agenda.getDate()
        );
    }
}