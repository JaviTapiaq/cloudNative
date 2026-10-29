package com.pedidos360.mensajeria.admin.dto;

import com.pedidos360.mensajeria.admin.validation.NombreRecurso;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Solicitud para crear un exchange.
 *
 * @param tipo direct, topic, fanout o headers (obligatorio)
 */
public record CrearExchangeRequest(
        @NotBlank @NombreRecurso String nombre,
        @NotNull TipoExchange tipo,
        Boolean durable,
        Boolean autoDelete
) {

    public boolean esDurable() {
        return durable == null || durable;
    }

    public boolean esAutoDelete() {
        return autoDelete != null && autoDelete;
    }
}
