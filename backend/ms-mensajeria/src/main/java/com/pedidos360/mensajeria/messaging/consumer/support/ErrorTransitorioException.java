package com.pedidos360.mensajeria.messaging.consumer.support;

/**
 * Error recuperable (servicio externo caido, timeout). El mensaje se reencola una vez;
 * si vuelve a fallar se envia a la DLQ.
 */
public class ErrorTransitorioException extends RuntimeException {

    public ErrorTransitorioException(String message) {
        super(message);
    }
}
