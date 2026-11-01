package com.zelia.agenda.exceptions;

import java.time.Instant;

public record ErrorResponse(int status, String mensagem, Instant timestamp) {
}
