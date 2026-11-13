package com.zelia.agenda.domain.agenda.dto;

import java.util.List;

public record AgendaResponseList(List<AgendaResponse> matutino, List<AgendaResponse> vespertino) {
}
