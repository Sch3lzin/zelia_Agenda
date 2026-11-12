package com.zelia.agenda.controllers;

import com.zelia.agenda.domain.agenda.Agenda;
import com.zelia.agenda.domain.agenda.dto.AgendaResponse;
import com.zelia.agenda.domain.agenda.dto.CreateAgendaDTO;
import com.zelia.agenda.service.AgendaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agenda")
public class AgendaController {

    private final AgendaService agendaService;

    public AgendaController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    @PostMapping
    public ResponseEntity<AgendaResponse> createAgenda(@AuthenticationPrincipal Long userId, @RequestBody @Valid CreateAgendaDTO dto) {
        Agenda agenda = agendaService.createAgenda(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(AgendaResponse.from(agenda));
    }
}
