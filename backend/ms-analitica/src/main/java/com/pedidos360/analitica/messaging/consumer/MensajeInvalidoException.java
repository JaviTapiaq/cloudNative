package com.pedidos360.analitica.messaging.consumer;

/**
 * Payload valido como JSON pero que no cumple el contrato del evento. Es un error permanente:
 * no se reintenta, el mensaje va directo al Dead Letter Topic.
 */
public class MensajeInvalidoException extends RuntimeException {

    public MensajeInvalidoException(String message) {
        super(message);
    }
}
