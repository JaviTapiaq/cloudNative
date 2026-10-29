package com.pedidos360.mensajeria.messaging.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Tarea asincrona: generar un ticket de atencion/seguimiento para un pedido.
 */
public record TicketMessage(
        @NotNull Long pedidoId,
        @NotBlank String cliente,
        @NotNull @PositiveOrZero BigDecimal total,
        @Pattern(regexp = "BAJA|MEDIA|ALTA", message = "debe ser BAJA, MEDIA o ALTA") String prioridad,
        String simularFallo
) implements MensajeSimulable {
}
