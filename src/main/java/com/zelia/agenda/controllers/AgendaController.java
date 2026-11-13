package com.zelia.agenda.controllers;

import com.zelia.agenda.domain.agenda.Agenda;
import com.zelia.agenda.domain.agenda.dto.AgendaResponse;
import com.zelia.agenda.domain.agenda.dto.AgendaResponseList;
import com.zelia.agenda.domain.agenda.dto.CreateAgendaDTO;
import com.zelia.agenda.domain.agenda.dto.GetListDayDTO;
import com.zelia.agenda.service.AgendaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/agenda")
public class AgendaController {

    private final AgendaService agendaService;

    public AgendaController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgendaResponse> getAgenda(@PathVariable Long id) {
        Agenda agenda = agendaService.getAgenda(id);
        return ResponseEntity.ok(AgendaResponse.from(agenda));
    }

    @PostMapping("/list")
    public ResponseEntity<AgendaResponseList> getListAgendaDay(@RequestBody @Valid GetListDayDTO dto) {
        AgendaResponseList agendaResponseList = agendaService.getListAgendaDay(dto);
        return ResponseEntity.ok(agendaResponseList);
    }

    @PostMapping
    public ResponseEntity<AgendaResponse> createAgenda(@AuthenticationPrincipal Long userId, @RequestBody @Valid CreateAgendaDTO dto) {
        Agenda agenda = agendaService.createAgenda(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(AgendaResponse.from(agenda));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgendaResponse> updateAgenda(@PathVariable Long id, @AuthenticationPrincipal Long userId, @RequestBody @Valid CreateAgendaDTO dto, Authentication authentication) {
        Agenda agenda = agendaService.updateAgenda(id, userId, dto, authentication);
        return ResponseEntity.ok(AgendaResponse.from(agenda));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAgenda(@PathVariable Long id ,@AuthenticationPrincipal Long userId, Authentication authentication) {
        agendaService.deleteAgenda(id, userId, authentication);
        return ResponseEntity.noContent().build();
    }
}
