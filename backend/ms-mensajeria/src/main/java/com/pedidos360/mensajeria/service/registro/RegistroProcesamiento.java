package com.pedidos360.mensajeria.service.registro;

import java.time.Instant;

public record RegistroProcesamiento(
        Instant fecha,
        String messageId,
        String cola,
        EstadoProcesamiento estado,
        String detalle
) {

    public enum EstadoProcesamiento {
        PROCESADO,
        REENCOLADO,
        ENVIADO_A_DLQ,
        RECIBIDO_EN_DLQ
    }
}
