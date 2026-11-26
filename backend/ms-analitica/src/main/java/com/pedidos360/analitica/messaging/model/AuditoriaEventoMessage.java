package com.pedidos360.analitica.messaging.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/**
 * Contrato JSON del registro de auditoria (topico pedidos360.auditoria.eventos).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuditoriaEventoMessage(
        @NotBlank String eventId,
        @NotBlank String accion,
        @NotBlank String entidad,
        @NotBlank String entidadId,
        String actor,
        String detalle,
        String origen,
        LocalDateTime ocurridoEn
) {
}
