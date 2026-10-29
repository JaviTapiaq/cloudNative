package com.pedidos360.mensajeria.messaging.consumer.support;

/**
 * Error permanente: el mensaje nunca podra procesarse (JSON mal formado, campos invalidos,
 * evento desconocido). Se rechaza sin reencolar y RabbitMQ lo deriva a la DLQ.
 */
public class MensajeInvalidoException extends RuntimeException {

    public MensajeInvalidoException(String message) {
        super(message);
    }

    public MensajeInvalidoException(String message, Throwable cause) {
        super(message, cause);
    }
}
