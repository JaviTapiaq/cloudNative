package com.pedidos360.mensajeria.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pedidos360.mensajeria.admin.validation.NombreRecurso;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Solicitud para crear una cola.
 *
 * @param nombre      nombre de la cola (obligatorio)
 * @param durable     sobrevive a reinicios del broker (por defecto true)
 * @param autoDelete  se elimina al desconectarse el ultimo consumidor (por defecto false)
 * @param ttlMs       tiempo de vida de los mensajes en ms (1 ms a 14 dias)
 * @param maxMensajes largo maximo de la cola (1 a 1.000.000)
 * @param crearDlq    crea "{nombre}.dlq" enlazada al dead letter exchange del sistema
 * @param exchange    exchange existente al que enlazar la cola (opcional, requiere routingKey)
 * @param routingKey  routing key del enlace inicial (opcional, requiere exchange)
 */
public record CrearColaRequest(
        @NotBlank @NombreRecurso String nombre,
        Boolean durable,
        Boolean autoDelete,
        @Positive @Max(1_209_600_000) Integer ttlMs,
        @Positive @Max(1_000_000) Integer maxMensajes,
        Boolean crearDlq,
        @NombreRecurso String exchange,
        @Size(max = 255) @Pattern(regexp = "^[A-Za-z0-9._*#-]*$",
                message = "solo admite [A-Za-z0-9._-] y los comodines * #") String routingKey
) {

    public boolean esDurable() {
        return durable == null || durable;
    }

    public boolean esAutoDelete() {
        return autoDelete != null && autoDelete;
    }

    public boolean conDlq() {
        return crearDlq != null && crearDlq;
    }

    @JsonIgnore
    @AssertTrue(message = "exchange y routingKey deben indicarse juntos")
    public boolean isBindingCompleto() {
        return (exchange == null) == (routingKey == null);
    }

    @JsonIgnore
    @AssertTrue(message = "el nombre no puede terminar en '.dlq' cuando crearDlq=true")
    public boolean isNombreCompatibleConDlq() {
        return !conDlq() || nombre == null || !nombre.endsWith(".dlq");
    }

    @JsonIgnore
    @AssertTrue(message = "una cola autoDelete no puede declararse con DLQ")
    public boolean isAutoDeleteCompatible() {
        return !(esAutoDelete() && conDlq());
    }
}
