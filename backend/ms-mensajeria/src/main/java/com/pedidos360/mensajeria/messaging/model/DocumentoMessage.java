package com.pedidos360.mensajeria.messaging.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Tarea asincrona: generar un documento asociado a un pedido (boleta, factura, guia de despacho).
 */
public record DocumentoMessage(
        @NotNull Long pedidoId,
        String cliente,
        @NotBlank @Pattern(regexp = "BOLETA|FACTURA|GUIA_DESPACHO",
                message = "debe ser BOLETA, FACTURA o GUIA_DESPACHO") String tipoDocumento,
        @Pattern(regexp = "PDF|TXT", message = "debe ser PDF o TXT") String formato,
        String simularFallo
) implements MensajeSimulable {
}
