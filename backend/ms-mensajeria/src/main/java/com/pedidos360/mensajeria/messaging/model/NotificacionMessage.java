package com.pedidos360.mensajeria.messaging.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Tarea asincrona: enviar una notificacion a un cliente.
 *
 * @param simularFallo solo para demostracion: PERMANENTE o TRANSITORIO fuerza un error en el consumidor.
 */
public record NotificacionMessage(
        @NotBlank @Size(max = 150) String destinatario,
        @NotBlank @Pattern(regexp = "EMAIL|SMS|PUSH", message = "debe ser EMAIL, SMS o PUSH") String canal,
        @NotBlank @Size(max = 200) String asunto,
        @NotBlank @Size(max = 2000) String cuerpo,
        Long pedidoId,
        String simularFallo
) implements MensajeSimulable {
}
