package com.pedidos360.productos.messaging.consumer.stock;

/**
 * El mensaje no puede procesarse nunca (JSON mal formado o campos invalidos): va a la DLQ.
 */
class MensajeInvalidoException extends RuntimeException {

    MensajeInvalidoException(String message) {
        super(message);
    }
}
